package bo.edu.uagrm.credenciales;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import bo.edu.uagrm.credenciales.security.AuthRateLimiter;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
@SpringBootTest(properties="app.security.rate-limit-enabled=true") @AutoConfigureMockMvc @ActiveProfiles("test")
class AuthRateIntegrationTests {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate jdbc;@Autowired AuthRateLimiter limiter;@Autowired com.fasterxml.jackson.databind.ObjectMapper json;
 @BeforeEach void cleanCounters(){jdbc.update("delete from auth_rate_limits");}
 @Test void loginLimitsNormalizeIdentityAndExposeRetryAfterWithoutSavingEmailOrIp()throws Exception{
  String email="rate-"+UUID.randomUUID()+"@example.test";
  for(int i=0;i<10;i++)mvc.perform(post("/api/auth/student/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email",i%2==0?email:email.toUpperCase(Locale.ROOT),"password","incorrect")))).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/student/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email",email,"password","incorrect"))))
   .andExpect(status().is(429)).andExpect(header().exists("Retry-After"));
  var keys=jdbc.queryForList("select bucket_key from auth_rate_limits",String.class);assertTrue(keys.stream().allMatch(k->k.matches("[a-f0-9]{64}")));
 }
 @Test void resetLimitKeepsUniform204ForKnownAndUnknownAccountsAndStopsCreatingTokens()throws Exception{
  String email="known-"+UUID.randomUUID()+"@example.test",unknown="unknown-"+UUID.randomUUID()+"@example.test";
  mvc.perform(post("/api/auth/student/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email",email,"password","RatePassword123!")))).andExpect(status().isCreated());
  for(String address:List.of(email,unknown))for(int i=0;i<5;i++)mvc.perform(post("/api/auth/student/password-reset/request").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email",address)))).andExpect(status().isNoContent());
  assertEquals(3,jdbc.queryForObject("select count(*) from student_account_tokens t join student_accounts s on s.id=t.student_id where s.email=? and t.token_type='PASSWORD_RESET'",Integer.class,email));
 }
 @Test void expiredWindowAllowsRequestsAgainWithoutWaiting() {
  assertTrue(limiter.take("expiry","identity",1,60).allowed());assertFalse(limiter.take("expiry","identity",1,60).allowed());
  jdbc.update("update auth_rate_limits set expires_at=?",java.sql.Timestamp.from(Instant.now().minusSeconds(1)));
  assertTrue(limiter.take("expiry","identity",1,60).allowed());
 }
 @Test void forwardedIpHeaderCannotBypassSocketAddressLimit()throws Exception{
  for(int i=0;i<120;i++)limiter.take("ip","127.0.0.1",120,60);
  mvc.perform(post("/api/auth/student/login").header("X-Forwarded-For","203.0.113.10").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"test@example.test\",\"password\":\"wrong\"}"))
    .andExpect(status().is(429)).andExpect(header().exists("Retry-After"));
 }
 @Test void concurrentUpdatesDoNotLoseAttempts()throws Exception{
  ExecutorService pool=Executors.newFixedThreadPool(8);CountDownLatch start=new CountDownLatch(1);List<Future<Boolean>> jobs=new ArrayList<>();
  try{for(int i=0;i<16;i++)jobs.add(pool.submit(()->{assertTrue(start.await(5,TimeUnit.SECONDS));return limiter.take("concurrent","one-account",5,60).allowed();}));start.countDown();int allowed=0;for(var job:jobs)if(job.get(15,TimeUnit.SECONDS))allowed++;
   assertTrue(allowed<=5);assertEquals(16,jdbc.queryForObject("select attempts from auth_rate_limits",Integer.class));
  }finally{start.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
 }
 @Test void oversizedAuthenticationPayloadIsRejectedBeforeParsing()throws Exception{
  mvc.perform(post("/api/auth/student/login").contentType(MediaType.APPLICATION_JSON).content("x".repeat(8193))).andExpect(status().is(413));
 }
}
