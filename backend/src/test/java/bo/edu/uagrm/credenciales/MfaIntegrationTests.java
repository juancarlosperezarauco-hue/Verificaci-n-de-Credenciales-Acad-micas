package bo.edu.uagrm.credenciales;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import bo.edu.uagrm.credenciales.security.*;
import bo.edu.uagrm.credenciales.user.*;
import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class MfaIntegrationTests {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired AppUserRepository users;@Autowired PasswordEncoder encoder;@Autowired JwtService jwt;@Autowired MfaTotp totp;@Autowired JdbcTemplate jdbc;
 @MockitoBean Clock clock;
 final String password="MfaTestPassword123!";
 record Account(UUID id,String email,String token){}
 @BeforeEach void clock(){when(clock.instant()).thenReturn(Instant.parse("2026-11-14T12:00:00Z"));}
 Account root(){var user=users.saveAndFlush(new AppUser(null,UUID.randomUUID()+"@root.example",encoder.encode(password),UserRole.SUPER_ADMIN));return new Account(user.getId(),user.getEmail(),jwt.create(UserPrincipal.from(user)));}
 JsonNode postJson(String path,String token,Object body,int code)throws Exception{String text=mvc.perform(post(path).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().is(code)).andReturn().getResponse().getContentAsString();return text.isBlank()?json.nullNode():json.readTree(text);}
 JsonNode challenge(Account a)throws Exception{return postJson("/api/auth/platform/login","",Map.of("email",a.email(),"password",password),200);}
 JsonNode enable(Account a)throws Exception{var prepared=postJson("/api/security/mfa/setup",a.token(),Map.of("password",password),200);String secret=prepared.get("secret").asText();String code=totp.code(MfaTotp.decode(secret),totp.counter(),6);return postJson("/api/security/mfa/confirm",a.token(),Map.of("code",code),200);}
 void access(String token,int statusCode)throws Exception{mvc.perform(get("/api/access/me").header("Authorization","Bearer "+token)).andExpect(status().is(statusCode));}
 @Test void enrollmentRequiresPasswordAndLoginIssuesNoJwtBeforeMfa()throws Exception{
  Account a=root();postJson("/api/security/mfa/setup",a.token(),Map.of("password","incorrect"),401);
  var setup=postJson("/api/security/mfa/setup",a.token(),Map.of("password",password),200);String seed=setup.get("secret").asText();
  String encrypted=jdbc.queryForObject("select encrypted_secret from user_mfa where user_id=?",String.class,a.id());assertNotEquals(seed,encrypted);
  String code=totp.code(MfaTotp.decode(seed),totp.counter(),6);var enabled=postJson("/api/security/mfa/confirm",a.token(),Map.of("code",code),200);
  assertEquals(10,enabled.get("recoveryCodes").size());access(a.token(),403);access(enabled.get("session").get("token").asText(),200);
  var challenge=challenge(a);assertTrue(challenge.get("mfaRequired").asBoolean());assertFalse(challenge.has("token"));
  var verified=postJson("/api/auth/mfa/verify","",Map.of("challengeToken",challenge.get("challengeToken").asText(),"code",totp.code(MfaTotp.decode(seed),totp.counter()+1,6)),200);access(verified.get("token").asText(),200);
  postJson("/api/auth/mfa/verify","",Map.of("challengeToken",challenge.get("challengeToken").asText(),"code",enabled.get("recoveryCodes").get(0).asText()),401);
  var next=challenge(a);postJson("/api/auth/mfa/verify","",Map.of("challengeToken",next.get("challengeToken").asText(),"code",totp.code(MfaTotp.decode(seed),totp.counter()+1,6)),401);
 }
 @Test void failuresPersistAndFiveAttemptsOrExpirationBlockEvenAValidRecoveryCode()throws Exception{
  Account a=root();var enabled=enable(a);var c=challenge(a);String raw=c.get("challengeToken").asText();
  for(int i=0;i<5;i++)postJson("/api/auth/mfa/verify","",Map.of("challengeToken",raw,"code","invalid"),401);
  assertEquals(5,jdbc.queryForObject("select attempts from mfa_challenges where token_hash=?",Integer.class,MfaCrypto.hash(raw)));
  postJson("/api/auth/mfa/verify","",Map.of("challengeToken",raw,"code",enabled.get("recoveryCodes").get(0).asText()),401);
  var expired=challenge(a);String exp=expired.get("challengeToken").asText();jdbc.update("update mfa_challenges set expires_at=? where token_hash=?",java.sql.Timestamp.from(Instant.now().minusSeconds(1)),MfaCrypto.hash(exp));
  postJson("/api/auth/mfa/verify","",Map.of("challengeToken",exp,"code",enabled.get("recoveryCodes").get(0).asText()),401);
 }
 @Test void recoveryCodesAreHashedAndWorkOnlyOnceAcrossDifferentChallenges()throws Exception{
  Account a=root();var enabled=enable(a);String recovery=enabled.get("recoveryCodes").get(0).asText();
  assertEquals(0,jdbc.queryForObject("select count(*) from mfa_recovery_codes where code_hash=?",Integer.class,recovery));
  var c=challenge(a);postJson("/api/auth/mfa/verify","",Map.of("challengeToken",c.get("challengeToken").asText(),"code",recovery),200);
  c=challenge(a);postJson("/api/auth/mfa/verify","",Map.of("challengeToken",c.get("challengeToken").asText(),"code",recovery),401);
  postJson("/api/auth/mfa/verify","",Map.of("challengeToken",c.get("challengeToken").asText(),"code",enabled.get("recoveryCodes").get(1).asText()),200);
 }
 @Test void disablingNeedsPasswordAndSecondFactorAndRevokesAllSessions()throws Exception{
  Account a=root();var enabled=enable(a);String token=enabled.get("session").get("token").asText(),code=enabled.get("recoveryCodes").get(0).asText();
  mvc.perform(delete("/api/security/mfa").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("password",password,"code","wrong")))).andExpect(status().isUnauthorized());access(token,200);
  mvc.perform(delete("/api/security/mfa").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("password",password,"code",code)))).andExpect(status().isNoContent());access(token,403);
  var login=challenge(a);assertTrue(login.has("token"));assertFalse(login.has("mfaRequired"));
 }
 @Test void studentsCannotEnrollAdministrativeMfa()throws Exception{
  var s=postJson("/api/auth/student/register","",Map.of("email",UUID.randomUUID()+"@student.example","password",password),201);
  postJson("/api/security/mfa/setup",s.get("token").asText(),Map.of("password",password),403);
 }
 @Test void reenrollmentInvalidatesChallengesFromThePreviousAuthenticator()throws Exception{
  Account a=root();var first=enable(a);String oldChallenge=challenge(a).get("challengeToken").asText();
  mvc.perform(delete("/api/security/mfa").header("Authorization","Bearer "+first.get("session").get("token").asText()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("password",password,"code",first.get("recoveryCodes").get(0).asText())))).andExpect(status().isNoContent());
  String newSession=challenge(a).get("token").asText();var second=enable(new Account(a.id(),a.email(),newSession));
  String recovery=second.get("recoveryCodes").get(0).asText();
  postJson("/api/auth/mfa/verify","",Map.of("challengeToken",oldChallenge,"code",recovery),401);
  postJson("/api/auth/mfa/verify","",Map.of("challengeToken",challenge(a).get("challengeToken").asText(),"code",recovery),200);
 }
 @Test void suspendedTenantCannotFinishAnAlreadyStartedMfaLogin()throws Exception{
  String slug="mfa-"+UUID.randomUUID();var owner=postJson("/api/auth/register","",Map.of("organizationName","Institución MFA","organizationSlug",slug,"institutionType","ACADEMIA","email","admin@test.example","password",password),201);
  UUID tenantId=UUID.fromString(owner.get("tenantId").asText());UUID userId=jdbc.queryForObject("select id from app_users where tenant_id=?",UUID.class,tenantId);
  var enabled=enable(new Account(userId,"admin@test.example",owner.get("token").asText()));
  var pending=postJson("/api/auth/login","",Map.of("organizationSlug",slug,"email","admin@test.example","password",password),200);
  jdbc.update("update organization_governance set status='SUSPENDED' where tenant_id=?",tenantId);
  jdbc.update("update tenants set active=false where id=?",tenantId);
  postJson("/api/auth/mfa/verify","",Map.of("challengeToken",pending.get("challengeToken").asText(),"code",enabled.get("recoveryCodes").get(0).asText()),401);
 }
 @Test void concurrentChallengeConsumptionCreatesOnlyOneSuccessfulLogin()throws Exception{
  Account a=root();var enabled=enable(a);String raw=challenge(a).get("challengeToken").asText(),code=enabled.get("recoveryCodes").get(0).asText();
  ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
  Callable<Integer> attempt=()->{assertTrue(start.await(5,TimeUnit.SECONDS));return mvc.perform(post("/api/auth/mfa/verify").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("challengeToken",raw,"code",code)))).andReturn().getResponse().getStatus();};
  try{var x=pool.submit(attempt);var y=pool.submit(attempt);start.countDown();var statuses=new ArrayList<>(List.of(x.get(15,TimeUnit.SECONDS),y.get(15,TimeUnit.SECONDS)));Collections.sort(statuses);assertEquals(List.of(200,401),statuses);}finally{start.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
 }
}
