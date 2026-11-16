package bo.edu.uagrm.credenciales;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import bo.edu.uagrm.credenciales.security.*;
import bo.edu.uagrm.credenciales.user.*;
import bo.edu.uagrm.credenciales.mail.*;
import com.fasterxml.jackson.databind.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
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
class SecurityPolishIntegrationTests {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;@Autowired AppUserRepository users;@Autowired PasswordEncoder passwords;@Autowired JwtService jwt;@Autowired MfaTotp totp;@Autowired MfaCrypto crypto;@Autowired SecurityMaintenance maintenance;@Autowired MailOutboxStore mail;
 @MockitoBean Clock clock;
 final String password="PolishTestPassword123!";Instant now;
 record Root(UUID id,String email,String token){}
 @BeforeEach void time(){now=Instant.now();when(clock.instant()).thenReturn(now);}
 Root root(){var u=users.saveAndFlush(new AppUser(null,UUID.randomUUID()+"@root.test",passwords.encode(password),UserRole.SUPER_ADMIN));return new Root(u.getId(),u.getEmail(),jwt.create(UserPrincipal.from(u)));}
 JsonNode postJson(String path,String token,Object body,int code)throws Exception{String text=mvc.perform(post(path).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().is(code)).andReturn().getResponse().getContentAsString();return text.isBlank()?json.nullNode():json.readTree(text);}
 JsonNode login(Root a)throws Exception{return postJson("/api/auth/platform/login","",Map.of("email",a.email,"password",password),200);}
 JsonNode enable(Root a)throws Exception{var setup=postJson("/api/security/mfa/setup",a.token,Map.of("password",password),200);return postJson("/api/security/mfa/confirm",a.token,Map.of("code",totp.code(MfaTotp.decode(setup.get("secret").asText()),totp.counter(),6)),200);}
 void access(String token,int code)throws Exception{mvc.perform(get("/api/access/me").header("Authorization","Bearer "+token)).andExpect(status().is(code));}
 @Test void renewalRequiresTwoFactorsInvalidatesOldBackupsChallengesAndSessions()throws Exception{
  var a=root();var first=enable(a);String token=first.get("session").get("token").asText();var pending=login(a);
  postJson("/api/security/mfa/recovery-codes",token,Map.of("password","wrong","code",first.get("recoveryCodes").get(0).asText()),401);
  postJson("/api/security/mfa/recovery-codes",token,Map.of("password",password,"code","invalid"),401);
  var next=postJson("/api/security/mfa/recovery-codes",token,Map.of("password",password,"code",first.get("recoveryCodes").get(0).asText()),200);assertEquals(10,next.get("recoveryCodes").size());access(token,403);access(next.get("session").get("token").asText(),200);
  postJson("/api/auth/mfa/verify","",Map.of("challengeToken",pending.get("challengeToken").asText(),"code",next.get("recoveryCodes").get(0).asText()),401);
  var challenge=login(a);postJson("/api/auth/mfa/verify","",Map.of("challengeToken",challenge.get("challengeToken").asText(),"code",first.get("recoveryCodes").get(1).asText()),401);postJson("/api/auth/mfa/verify","",Map.of("challengeToken",challenge.get("challengeToken").asText(),"code",next.get("recoveryCodes").get(0).asText()),200);
 }
 @Test void replacementKeepsCurrentMfaUntilConfirmationThenSwitchesTheSecret()throws Exception{
  var a=root();var first=enable(a);String token=first.get("session").get("token").asText(),old=jdbc.queryForObject("select encrypted_secret from user_mfa where user_id=?",String.class,a.id);
  var setup=postJson("/api/security/mfa/replacement",token,Map.of("password",password,"code",first.get("recoveryCodes").get(0).asText()),200);assertEquals(old,jdbc.queryForObject("select encrypted_secret from user_mfa where user_id=?",String.class,a.id));
  var still=login(a);assertTrue(still.get("mfaRequired").asBoolean());postJson("/api/auth/mfa/verify","",Map.of("challengeToken",still.get("challengeToken").asText(),"code",first.get("recoveryCodes").get(1).asText()),200);
  String seed=setup.get("secret").asText();var changed=postJson("/api/security/mfa/confirm",token,Map.of("code",totp.code(MfaTotp.decode(seed),totp.counter(),6)),200);access(token,403);assertEquals(seed,crypto.decrypt(a.id,jdbc.queryForObject("select encrypted_secret from user_mfa where user_id=?",String.class,a.id)));assertNull(jdbc.queryForObject("select pending_secret from user_mfa where user_id=?",String.class,a.id));
  var challenge=login(a);postJson("/api/auth/mfa/verify","",Map.of("challengeToken",challenge.get("challengeToken").asText(),"code",first.get("recoveryCodes").get(2).asText()),401);access(changed.get("session").get("token").asText(),200);
 }
 @Test void cancelAndExpiredPreparationDoNotDisableTheExistingAuthenticator()throws Exception{
  var a=root();var enabled=enable(a);String token=enabled.get("session").get("token").asText();postJson("/api/security/mfa/replacement",token,Map.of("password",password,"code",enabled.get("recoveryCodes").get(0).asText()),200);
  mvc.perform(delete("/api/security/mfa/pending").header("Authorization","Bearer "+token)).andExpect(status().isNoContent());assertTrue(login(a).get("mfaRequired").asBoolean());
  postJson("/api/security/mfa/replacement",token,Map.of("password",password,"code",enabled.get("recoveryCodes").get(1).asText()),200);jdbc.update("update user_mfa set pending_until=? where user_id=?",Timestamp.from(now.minusSeconds(1)),a.id);assertTrue(maintenance.cleanup().preparations()>0);assertNull(jdbc.queryForObject("select pending_secret from user_mfa where user_id=?",String.class,a.id));assertTrue(login(a).get("mfaRequired").asBoolean());access(token,200);
 }
 @Test void retentionKeepsActiveSessionsAccountsUsableBackupsPendingMailAndAudit()throws Exception{
  var a=root();var enabled=enable(a);String current=enabled.get("session").get("token").asText();Timestamp old=Timestamp.from(now.minus(Duration.ofDays(31)));
  jdbc.update("update login_sessions set revoked_at=?,expires_at=? where user_id=? and revoked_at is not null",old,old,a.id);
  var newer=postJson("/api/security/mfa/recovery-codes",current,Map.of("password",password,"code",enabled.get("recoveryCodes").get(0).asText()),200);current=newer.get("session").get("token").asText();jdbc.update("update mfa_recovery_codes set used_at=? where user_id=? and used=true",old,a.id);
  var student=postJson("/api/auth/student/register","",Map.of("email",UUID.randomUUID()+"@retention.test","password",password),201);UUID sid=UUID.fromString(com.auth0.jwt.JWT.decode(student.get("token").asText()).getSubject());jdbc.update("update student_account_tokens set expires_at=? where student_id=?",old,sid);
  int auditBefore=jdbc.queryForObject("select count(*) from audit_events where resource_id=?",Integer.class,sid);var result=maintenance.cleanup();assertTrue(result.sessions()>0);assertTrue(result.backups()>0);access(current,200);access(student.get("token").asText(),200);assertEquals(10,jdbc.queryForObject("select count(*) from mfa_recovery_codes where user_id=? and used=false",Integer.class,a.id));assertEquals(1,jdbc.queryForObject("select count(*) from student_account_tokens where student_id=?",Integer.class,sid));assertEquals(auditBefore,jdbc.queryForObject("select count(*) from audit_events where resource_id=?",Integer.class,sid));
 }
 @Test void terminalMailIsRemovedBeforeItsExpiredReferencedToken()throws Exception{
  var s=postJson("/api/auth/student/register","",Map.of("email",UUID.randomUUID()+"@purge.test","password",password),201);UUID sid=UUID.fromString(com.auth0.jwt.JWT.decode(s.get("token").asText()).getSubject());UUID id=jdbc.queryForObject("select id from mail_outbox where student_id=?",UUID.class,sid);jdbc.update("update mail_outbox set next_attempt_at=? where id=?",Timestamp.from(now.minusSeconds(1)),id);var claim=mail.claim(id).orElseThrow();mail.finish(claim,MailState.SENT,null);Timestamp old=Timestamp.from(now.minus(Duration.ofDays(31)));jdbc.update("update mail_outbox set terminal_at=? where id=?",old,id);jdbc.update("update student_account_tokens set expires_at=? where student_id=?",old,sid);var result=maintenance.cleanup();assertTrue(result.mail()>0);assertTrue(result.tokens()>0);assertEquals(1,jdbc.queryForObject("select count(*) from student_accounts where id=?",Integer.class,sid));access(s.get("token").asText(),200);
 }
 @Test void maintenanceDoesNotExposeSecretsAndOnlySuperadminCanUseIt()throws Exception{
  var s=postJson("/api/auth/student/register","",Map.of("email",UUID.randomUUID()+"@scope.test","password",password),201);mvc.perform(get("/api/platform/security/maintenance").header("Authorization","Bearer "+s.get("token").asText())).andExpect(status().isForbidden());var a=root();var response=mvc.perform(get("/api/platform/security/maintenance").header("Authorization","Bearer "+a.token)).andExpect(status().isOk()).andExpect(jsonPath("$.retentionDays").value(30)).andExpect(jsonPath("$.jwtKeyId").value("legacy")).andReturn().getResponse().getContentAsString();assertFalse(response.contains("encryption-key"));assertFalse(response.contains("AAAAAAA"));
 }
 @Test void renewalDoesNotRefreshTerminalRecordsAndDelayTheirRetentionDeadline()throws Exception{
  var a=root();var enabled=enable(a);Instant old=now.minus(Duration.ofDays(31)).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);String hash=MfaCrypto.hash(a.id+":"+enabled.get("recoveryCodes").get(0).asText());UUID sessionId=UUID.fromString(com.auth0.jwt.JWT.decode(a.token).getId());
  jdbc.update("update mfa_recovery_codes set used=true,used_at=? where code_hash=?",Timestamp.from(old),hash);jdbc.update("update login_sessions set revoked_at=? where id=?",Timestamp.from(old),sessionId);
  postJson("/api/security/mfa/recovery-codes",enabled.get("session").get("token").asText(),Map.of("password",password,"code",enabled.get("recoveryCodes").get(1).asText()),200);
  assertEquals(old,jdbc.queryForObject("select used_at from mfa_recovery_codes where code_hash=?",(r,n)->r.getTimestamp(1).toInstant(),hash));assertEquals(old,jdbc.queryForObject("select revoked_at from login_sessions where id=?",(r,n)->r.getTimestamp(1).toInstant(),sessionId));
 }
}
