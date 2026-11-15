package bo.edu.uagrm.credenciales.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MailCipher {
 public record Payload(String to,String subject,String body){@Override public String toString(){return "MailPayload[redacted]";}}
 private final SecretKeySpec key;private final ObjectMapper json;private static final SecureRandom RANDOM=new SecureRandom();
 public MailCipher(@Value("${app.mail-outbox.encryption-key:}") String encoded,ObjectMapper json){
  byte[] bytes;try{bytes=Base64.getDecoder().decode(encoded);}catch(IllegalArgumentException e){throw new IllegalArgumentException("MAIL_ENCRYPTION_KEY debe ser base64 de 32 bytes");}
  if(bytes.length!=32)throw new IllegalArgumentException("Configura MAIL_ENCRYPTION_KEY independiente, 32 bytes en base64");
  key=new SecretKeySpec(bytes,"AES");this.json=json;
 }
 public String encrypt(UUID id,Payload payload){
  try{byte[] nonce=new byte[12];RANDOM.nextBytes(nonce);var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,nonce));cipher.updateAAD(aad(id));byte[] value=cipher.doFinal(json.writeValueAsBytes(payload));return Base64.getEncoder().encodeToString(ByteBuffer.allocate(12+value.length).put(nonce).put(value).array());}
  catch(Exception e){throw new IllegalStateException("No se pudo proteger el correo");}
 }
 public Payload decrypt(UUID id,String encoded){
  try{byte[] value=Base64.getDecoder().decode(encoded);if(value.length<29)throw new IllegalArgumentException();var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Arrays.copyOf(value,12)));cipher.updateAAD(aad(id));return json.readValue(cipher.doFinal(value,12,value.length-12),Payload.class);}
  catch(Exception e){throw new IllegalStateException("No se pudo leer el correo protegido");}
 }
 private byte[] aad(UUID id){return ("mail-outbox:v1:"+id).getBytes(StandardCharsets.UTF_8);}
}
