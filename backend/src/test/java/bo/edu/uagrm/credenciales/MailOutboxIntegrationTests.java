package bo.edu.uagrm.credenciales;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import bo.edu.uagrm.credenciales.mail.*;
import bo.edu.uagrm.credenciales.identity.*;
import bo.edu.uagrm.credenciales.security.*;
import bo.edu.uagrm.credenciales.user.*;
import com.fasterxml.jackson.databind.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest(properties="app.mail-outbox.max-attempts=3") @AutoConfigureMockMvc @ActiveProfiles("test")
class MailOutboxIntegrationTests {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;@Autowired MailOutboxWorker worker;@Autowired MailOutboxStore store;@Autowired MailCipher cipher;
 @Autowired IdentityService identity;@Autowired StudentAccountRepository accounts;@Autowired PlatformTransactionManager transactions;@Autowired AppUserRepository users;@Autowired JwtService jwt;
 @MockitoBean JavaMailSender smtp;
 final String password="OutboxTestPassword123!";
 record Fixture(UUID student,UUID mail,String token,String email){}
 Fixture account()throws Exception{
  String email=UUID.randomUUID()+"@outbox.test";
  var response=json.readTree(mvc.perform(post("/api/auth/student/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email",email,"password",password)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
  UUID student=accounts.findByEmail(email).orElseThrow().getId();UUID id=jdbc.queryForObject("select id from mail_outbox where student_id=?",UUID.class,student);return new Fixture(student,id,response.get("token").asText(),email);
 }
 String state(Fixture a){return jdbc.queryForObject("select state from mail_outbox where id=?",String.class,a.mail);}
 String payload(Fixture a){return jdbc.queryForObject("select encrypted_payload from mail_outbox where id=?",String.class,a.mail);}
 void due(Fixture a){jdbc.update("update mail_outbox set next_attempt_at=? where id=?",Timestamp.from(Instant.now().minusSeconds(1)),a.mail);}
 @Test void enqueueIsEncryptedAndTokenAuditAndQueueRollBackTogether()throws Exception{
  var a=account();String encrypted=payload(a);assertEquals("PENDING",state(a));assertFalse(encrypted.contains(a.email));assertFalse(encrypted.contains("token="));var plain=cipher.decrypt(a.mail,encrypted);assertEquals(a.email,plain.to());assertTrue(plain.body().contains("?type=verification&token="));assertFalse(plain.toString().contains(a.email));
  AtomicReference<UUID> owner=new AtomicReference<>();String rolled=UUID.randomUUID()+"@rollback.test";
  new TransactionTemplate(transactions).executeWithoutResult(tx->{identity.register(json.convertValue(Map.of("email",rolled,"password",password),IdentityDtos.Register.class));owner.set(accounts.findByEmail(rolled).orElseThrow().getId());tx.setRollbackOnly();});
  assertTrue(accounts.findByEmail(rolled).isEmpty());assertEquals(0,jdbc.queryForObject("select count(*) from mail_outbox where student_id=?",Integer.class,owner.get()));assertEquals(0,jdbc.queryForObject("select count(*) from audit_events where resource_id=?",Integer.class,owner.get()));verifyNoInteractions(smtp);
 }
 @Test void smtpFailureRetriesSameMessageAndClearsPayloadOnlyAfterAcceptance()throws Exception{
  var a=account();String original=cipher.decrypt(a.mail,payload(a)).body();AtomicReference<String> received=new AtomicReference<>();
  doThrow(new MailSendException("private-provider-error "+a.email)).doAnswer(call->{received.set(call.getArgument(0,SimpleMailMessage.class).getText());return null;}).when(smtp).send(any(SimpleMailMessage.class));
  worker.dispatch(a.mail);assertEquals("RETRY",state(a));assertEquals("DELIVERY_FAILED",jdbc.queryForObject("select last_error from mail_outbox where id=?",String.class,a.mail));assertNotNull(payload(a));
  Instant next=jdbc.queryForObject("select next_attempt_at from mail_outbox where id=?",(r,n)->r.getTimestamp(1).toInstant(),a.mail);assertTrue(next.isAfter(Instant.now().plusSeconds(20)));
  worker.dispatch(a.mail);verify(smtp,times(1)).send(any(SimpleMailMessage.class));due(a);worker.dispatch(a.mail);assertEquals("SENT",state(a));assertEquals(original,received.get());assertNull(payload(a));worker.dispatch(a.mail);verify(smtp,times(2)).send(any(SimpleMailMessage.class));
 }
 @Test void attemptBudgetTerminatesAndDoesNotRetainTheActionLink()throws Exception{
  var a=account();doThrow(new MailSendException("provider secret")).when(smtp).send(any(SimpleMailMessage.class));for(int i=0;i<3;i++){due(a);worker.dispatch(a.mail);}
  assertEquals("FAILED",state(a));assertNull(payload(a));assertEquals(3,jdbc.queryForObject("select attempts from mail_outbox where id=?",Integer.class,a.mail));due(a);worker.dispatch(a.mail);verify(smtp,times(3)).send(any(SimpleMailMessage.class));
 }
 @Test void consumedExpiredAndInactiveAccountsCancelBeforeSmtp()throws Exception{
  var used=account();jdbc.update("update student_account_tokens set used_at=? where student_id=?",Timestamp.from(Instant.now()),used.student);worker.dispatch(used.mail);assertEquals("CANCELLED",state(used));assertNull(payload(used));
  var expired=account();jdbc.update("update mail_outbox set expires_at=? where id=?",Timestamp.from(Instant.now().minusSeconds(1)),expired.mail);worker.dispatch(expired.mail);assertEquals("CANCELLED",state(expired));
  var inactive=account();jdbc.update("update student_accounts set active=false where id=?",inactive.student);worker.dispatch(inactive.mail);assertEquals("CANCELLED",state(inactive));verifyNoInteractions(smtp);
 }
 @Test void concurrentClaimsAndExpiredLeaseCannotBeCompletedByAStaleWorker()throws Exception{
  var a=account();ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
  Callable<Optional<MailOutboxStore.Delivery>> claim=()->{assertTrue(start.await(5,TimeUnit.SECONDS));return store.claim(a.mail);};
  MailOutboxStore.Delivery first;
  try{var x=pool.submit(claim);var y=pool.submit(claim);start.countDown();var results=List.of(x.get(10,TimeUnit.SECONDS),y.get(10,TimeUnit.SECONDS));assertEquals(1,results.stream().filter(Optional::isPresent).count());first=results.stream().flatMap(Optional::stream).findFirst().orElseThrow();}finally{start.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
  jdbc.update("update mail_outbox set lease_until=? where id=?",Timestamp.from(Instant.now().minusSeconds(1)),a.mail);var second=store.claim(a.mail).orElseThrow();assertEquals(2,second.attempts());store.finish(first,MailState.SENT,null);assertEquals("PROCESSING",state(a));store.finish(second,MailState.SENT,null);assertEquals("SENT",state(a));assertNull(payload(a));
 }
 @Test void lastInterruptedLeaseBecomesFailedInsteadOfStayingProcessingForever()throws Exception{
  var a=account();assertTrue(store.claim(a.mail).isPresent());jdbc.update("update mail_outbox set attempts=3,lease_until=? where id=?",Timestamp.from(Instant.now().minusSeconds(1)),a.mail);assertTrue(store.claim(a.mail).isEmpty());assertEquals("FAILED",state(a));assertNull(payload(a));
 }
 @Test void monitoringRequiresSuperadminAndExposesOnlyMetadata()throws Exception{
  var a=account();mvc.perform(get("/api/platform/mail").header("Authorization","Bearer "+a.token)).andExpect(status().isForbidden());
  var root=users.saveAndFlush(new AppUser(null,UUID.randomUUID()+"@root.test","not-a-real-password-hash",UserRole.SUPER_ADMIN));String token=jwt.create(UserPrincipal.from(root));
  String response=mvc.perform(get("/api/platform/mail").param("state","PENDING").param("size","100").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertFalse(response.contains(a.email));assertFalse(response.contains("encryptedPayload"));assertFalse(response.contains("tokenId"));assertFalse(response.contains(payload(a)));
  mvc.perform(get("/api/platform/mail").param("size","101").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
 }
 @Test void encryptedPayloadCannotBeReadWithAnotherQueueIdOrAfterTampering()throws Exception{
  var a=account();String value=payload(a);assertThrows(IllegalStateException.class,()->cipher.decrypt(UUID.randomUUID(),value));byte[] raw=Base64.getDecoder().decode(value);raw[20]^=1;assertThrows(IllegalStateException.class,()->cipher.decrypt(a.mail,Base64.getEncoder().encodeToString(raw)));
 }
}
