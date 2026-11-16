package bo.edu.uagrm.credenciales;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class SessionIntegrationTests {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;@Autowired org.springframework.core.env.Environment env;
 private final String password="SessionTestPassword123!";
 private JsonNode postJson(String path,String token,Object body,int code)throws Exception{return json.readTree(mvc.perform(post(path).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().is(code)).andReturn().getResponse().getContentAsString());}
 private JsonNode register(String email)throws Exception{return postJson("/api/auth/student/register","",Map.of("email",email,"password",password),201);}
 private JsonNode sessions(String token)throws Exception{return json.readTree(mvc.perform(get("/api/security/sessions").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
 private void profile(String token,int statusCode)throws Exception{mvc.perform(get("/api/me/profile").header("Authorization","Bearer "+token)).andExpect(status().is(statusCode));}
 @Test void individualRevocationIsImmediateAndCannotTargetAnotherAccount()throws Exception{
  String email=UUID.randomUUID()+"@test.example",a=register(email).get("token").asText();
  String b=postJson("/api/auth/student/login","",Map.of("email",email,"password",password),200).get("token").asText();
  var list=sessions(b);assertEquals(2,list.size());String other="",current="";for(var s:list){if(s.get("current").asBoolean())current=s.get("id").asText();else other=s.get("id").asText();assertFalse(s.has("token"));assertFalse(s.has("email"));}
  String foreign=register(UUID.randomUUID()+"@test.example").get("token").asText();
  mvc.perform(delete("/api/security/sessions/{id}",current).header("Authorization","Bearer "+foreign)).andExpect(status().isNotFound());
  mvc.perform(delete("/api/security/sessions/{id}",other).header("Authorization","Bearer "+b)).andExpect(status().isNoContent());
  profile(a,403);profile(b,200);assertEquals(1,sessions(b).size());
 }
 @Test void revokeOthersPreservesCurrentAndLogoutClosesItOnTheServer()throws Exception{
  String email=UUID.randomUUID()+"@test.example",first=register(email).get("token").asText();
  String current=postJson("/api/auth/student/login","",Map.of("email",email,"password",password),200).get("token").asText();
  mvc.perform(post("/api/security/sessions/revoke-others").header("Authorization","Bearer "+current)).andExpect(status().isNoContent());
  profile(first,403);profile(current,200);
  mvc.perform(delete("/api/security/sessions/current").header("Authorization","Bearer "+current)).andExpect(status().isNoContent());profile(current,403);
 }
 @Test void expiredDatabaseSessionsAndLegacyTokensWithoutJtiCannotAuthenticate()throws Exception{
  String email=UUID.randomUUID()+"@test.example",token=register(email).get("token").asText();
  var session=sessions(token).get(0);jdbc.update("update login_sessions set expires_at=? where id=?",java.sql.Timestamp.from(Instant.now().minusSeconds(1)),UUID.fromString(session.get("id").asText()));profile(token,403);
  UUID student=jdbc.queryForObject("select id from student_accounts where email=?",UUID.class,email);
  String legacy=com.auth0.jwt.JWT.create().withIssuer("uagrm-credentials").withSubject(student.toString()).withClaim("accountType","STUDENT").withExpiresAt(Instant.now().plusSeconds(60)).sign(com.auth0.jwt.algorithms.Algorithm.HMAC256(env.getProperty("app.security.jwt-secret")));
  profile(legacy,403);
 }
 @Test void pendingInstitutionCanManageOnlyItsOwnSessions()throws Exception{
  var owner=postJson("/api/auth/register","",Map.of("organizationName","Institución Sesiones","organizationSlug","sessions-"+UUID.randomUUID(),"institutionType","ACADEMIA","email","admin@test.example","password",password),201);
  String token=owner.get("token").asText();assertEquals(1,sessions(token).size());
  mvc.perform(get("/api/credentials").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
 }
}
