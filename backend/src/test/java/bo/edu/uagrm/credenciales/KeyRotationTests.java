package bo.edu.uagrm.credenciales;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import bo.edu.uagrm.credenciales.security.*;
import bo.edu.uagrm.credenciales.mail.MailCipher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class KeyRotationTests {
 static String key(int value){byte[] bytes=new byte[32];Arrays.fill(bytes,(byte)value);return Base64.getEncoder().encodeToString(bytes);}
 @Test void seedsKeepLegacyCompatibilityAndCanBeReencryptedWithoutTheOldKey(){
  UUID owner=UUID.randomUUID();var old=new MfaCrypto(key(0));String legacy=old.encrypt(owner,"PRIVATE-SEED");var next=new MfaCrypto(key(1),"next","legacy="+key(0));
  assertEquals("PRIVATE-SEED",next.decrypt(owner,legacy));String rotated=next.encrypt(owner,next.decrypt(owner,legacy));assertTrue(rotated.startsWith("K1.next."));assertEquals("PRIVATE-SEED",new MfaCrypto(key(1),"next","").decrypt(owner,rotated));
  assertThrows(IllegalStateException.class,()->new MfaCrypto(key(1),"next","").decrypt(owner,legacy));assertThrows(IllegalStateException.class,()->next.decrypt(UUID.randomUUID(),rotated));
 }
 @Test void mailRotationPreservesTheMessageAndRejectsUnknownIds(){
  var json=new ObjectMapper();UUID id=UUID.randomUUID();var old=new MailCipher(key(1),json);var next=new MailCipher(key(2),json,"new-mail","legacy="+key(1));var body=new MailCipher.Payload("test@example.test","Test","private link");
  String legacy=old.encrypt(id,body);assertEquals(body,next.decrypt(id,legacy));String rotated=next.encrypt(id,next.decrypt(id,legacy));assertEquals(body,new MailCipher(key(2),json,"new-mail","").decrypt(id,rotated));assertThrows(IllegalStateException.class,()->next.decrypt(id,rotated.replace("new-mail","unknown")));
 }
 @Test void jwtUsesAllowlistedKidsAndKeepsOldSignaturesDuringTheGracePeriod(){
  String old="old-jwt-private-test-key-at-least-32-characters",current="new-jwt-private-test-key-at-least-32-characters";UUID owner=UUID.randomUUID(),session=UUID.randomUUID();
  var registry=mock(SessionRegistry.class);when(registry.open(any(),any(),eq("next"))).thenReturn(session);var next=new JwtService(current,60,registry,"next","legacy="+old);
  String legacy=JWT.create().withIssuer("uagrm-credentials").withSubject(owner.toString()).withJWTId(session.toString()).withExpiresAt(Instant.now().plusSeconds(300)).sign(Algorithm.HMAC256(old));assertEquals(owner,next.verify(legacy).userId());
  var principal=new UserPrincipal(owner,null,"test@example.test","",bo.edu.uagrm.credenciales.user.UserRole.SUPER_ADMIN,true);String token=next.create(principal);assertEquals("next",JWT.decode(token).getKeyId());assertEquals(session,next.verify(token).sessionId());
  assertThrows(JWTVerificationException.class,()->new JwtService(current,60,registry,"next","").verify(legacy));String unknown=JWT.create().withKeyId("https://untrusted.example/key").withIssuer("uagrm-credentials").sign(Algorithm.HMAC256(current));assertThrows(JWTVerificationException.class,()->next.verify(unknown));assertThrows(JWTVerificationException.class,()->next.verify(JWT.create().withKeyId("next").withIssuer("uagrm-credentials").sign(Algorithm.none())));
 }
 @Test void configurationRejectsAmbiguousIdentifiersAndWeakPreviousKeys(){
  assertThrows(IllegalArgumentException.class,()->new KeySet("legacy","secret","legacy=other"));assertThrows(IllegalArgumentException.class,()->new KeySet("bad_id","secret",""));assertThrows(IllegalArgumentException.class,()->new MfaCrypto("invalid"));assertThrows(IllegalArgumentException.class,()->new JwtService("test-current-private-key-long-enough-for-jwt",60,null,"next","legacy=short"));assertEquals("KeySet[redacted]",new KeySet("legacy","sensitive","").toString());
 }
}
