package bo.edu.uagrm.credenciales;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import bo.edu.uagrm.credenciales.security.*;
import bo.edu.uagrm.credenciales.mail.*;
import bo.edu.uagrm.credenciales.user.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties={"app.security.mfa-key-id=next","app.security.mfa-encryption-key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=","app.security.mfa-previous-keys=legacy=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","app.mail-outbox.key-id=next","app.mail-outbox.encryption-key=AgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgI=","app.mail-outbox.previous-keys=legacy=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="})
@AutoConfigureMockMvc @ActiveProfiles("test")
class KeyRotationIntegrationTests {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate jdbc;@Autowired ObjectMapper json;@Autowired AppUserRepository users;@Autowired JwtService jwt;@Autowired MfaCrypto mfa;@Autowired MailCipher cipher;@Autowired SecurityMaintenance maintenance;@Autowired MailOutboxStore mail;
 AppUser account(){return users.saveAndFlush(new AppUser(null,UUID.randomUUID()+"@root.test","test-only",UserRole.SUPER_ADMIN));}
 record Fixture(UUID user,UUID mail,String oldSeed,String oldMessage){}
 Fixture old()throws Exception{
  var user=account();String seed=new MfaCrypto(KeyRotationTests.key(0)).encrypt(user.getId(),"TEST-SEED");jdbc.update("insert into user_mfa(user_id,encrypted_secret,enabled,generation,last_counter) values(?,?,true,?,-1)",user.getId(),seed,UUID.randomUUID());
  var s=json.readTree(mvc.perform(post("/api/auth/student/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email",UUID.randomUUID()+"@rotation.test","password","RotationPassword123!")))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());UUID student=UUID.fromString(com.auth0.jwt.JWT.decode(s.get("token").asText()).getSubject());UUID id=jdbc.queryForObject("select id from mail_outbox where student_id=?",UUID.class,student);String value=jdbc.queryForObject("select encrypted_payload from mail_outbox where id=?",String.class,id);String legacy=new MailCipher(KeyRotationTests.key(1),json).encrypt(id,cipher.decrypt(id,value));jdbc.update("update mail_outbox set encrypted_payload=? where id=?",legacy,id);return new Fixture(user.getId(),id,seed,legacy);
 }
 String encrypted(UUID id){return jdbc.queryForObject("select encrypted_payload from mail_outbox where id=?",String.class,id);}
 @Test void batchesReencryptBothOwnersWithoutChangingTheirPlaintext()throws Exception{
  var f=old();for(int i=0;i<20&&!encrypted(f.mail).startsWith("K1.next.");i++)maintenance.reEncrypt();assertEquals("TEST-SEED",mfa.decrypt(f.user,jdbc.queryForObject("select encrypted_secret from user_mfa where user_id=?",String.class,f.user)));String newSeed=jdbc.queryForObject("select encrypted_secret from user_mfa where user_id=?",String.class,f.user);assertTrue(newSeed.startsWith("K1.next."));assertTrue(encrypted(f.mail).startsWith("K1.next."));assertEquals(cipher.decrypt(f.mail,f.oldMessage),new MailCipher(KeyRotationTests.key(2),json,"next","").decrypt(f.mail,encrypted(f.mail)));
 }
 @Test void processingPayloadIsNotRewrittenAndRetriesCanBeReencryptedAfterRelease()throws Exception{
  var f=old();var delivery=mail.claim(f.mail).orElseThrow();maintenance.reEncrypt();assertEquals(f.oldMessage,encrypted(f.mail));mail.finish(delivery,MailState.RETRY,"DELIVERY_FAILED");for(int i=0;i<20&&!encrypted(f.mail).startsWith("K1.next.");i++)maintenance.reEncrypt();assertTrue(encrypted(f.mail).startsWith("K1.next."));
 }
 @Test void unreadablePayloadRollsBackTheBatchIncludingItsAuditEvents()throws Exception{
  // Other integration fixtures may leave more than one bounded rotation batch.
  // Drain those before placing the corrupt row in the batch under test.
  for(int i=0;i<100&&mail.rotationPending()>0;i++)maintenance.reEncrypt();
  assertEquals(0,mail.rotationPending());
  var f=old();String valid=encrypted(f.mail);jdbc.update("update mail_outbox set encrypted_payload='invalid-ciphertext' where id=?",f.mail);
  try{assertThrows(IllegalStateException.class,()->maintenance.reEncrypt());assertEquals(f.oldSeed,jdbc.queryForObject("select encrypted_secret from user_mfa where user_id=?",String.class,f.user));assertEquals(0,jdbc.queryForObject("select count(*) from audit_events where resource_id=? and action='SECRET_REENCRYPTED'",Integer.class,f.user));}
  finally{jdbc.update("update mail_outbox set encrypted_payload=? where id=?",valid,f.mail);}
 }
}
