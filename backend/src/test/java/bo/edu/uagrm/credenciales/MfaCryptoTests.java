package bo.edu.uagrm.credenciales;
import static org.junit.jupiter.api.Assertions.*;
import bo.edu.uagrm.credenciales.security.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class MfaCryptoTests {
 @Test void matchesRfc6238Sha1VectorsIncludingTimesBeyond2038(){
  var otp=new MfaTotp(Clock.systemUTC());byte[] key="12345678901234567890".getBytes(StandardCharsets.US_ASCII);
  long[] times={59,1111111109,1111111111,1234567890,2000000000,20000000000L};
  String[] expected={"94287082","07081804","14050471","89005924","69279037","65353130"};
  for(int i=0;i<times.length;i++)assertEquals(expected[i],otp.code(key,times[i]/30,8));
 }
 @Test void acceptsOnlyUnusedTimeStepsInsideTheConfiguredWindow(){
  var otp=new MfaTotp(Clock.fixed(Instant.ofEpochSecond(3000),ZoneOffset.UTC));String secret=otp.secret();
  String code=otp.code(MfaTotp.decode(secret),100,6);
  assertEquals(100,otp.match(secret,code,-1));assertEquals(-1,otp.match(secret,code,100));
  assertEquals(-1,otp.match(secret,otp.code(MfaTotp.decode(secret),102,6),-1));
  assertEquals(-1,otp.match(secret,"not-a-code",-1));
 }
 @Test void encryptedSeedsAreBoundToTheirOwnerAndRejectTampering(){
  var crypto=new MfaCrypto(Base64.getEncoder().encodeToString(new byte[32]));UUID owner=UUID.randomUUID();
  String ciphertext=crypto.encrypt(owner,"TEST-SEED");assertNotEquals("TEST-SEED",ciphertext);assertEquals("TEST-SEED",crypto.decrypt(owner,ciphertext));
  assertThrows(IllegalStateException.class,()->crypto.decrypt(UUID.randomUUID(),ciphertext));
  byte[] changed=Base64.getDecoder().decode(ciphertext);changed[changed.length-1]^=1;
  assertThrows(IllegalStateException.class,()->crypto.decrypt(owner,Base64.getEncoder().encodeToString(changed)));
  assertNotEquals(ciphertext,crypto.encrypt(owner,"TEST-SEED"));
 }
}
