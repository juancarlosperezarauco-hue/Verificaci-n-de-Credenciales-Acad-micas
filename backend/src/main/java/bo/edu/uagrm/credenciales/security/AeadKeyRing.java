package bo.edu.uagrm.credenciales.security;

import java.nio.ByteBuffer;
import java.security.*;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;

public final class AeadKeyRing {
 private final KeySet keys;private final Map<String,SecretKeySpec> aes=new HashMap<>();private static final SecureRandom RANDOM=new SecureRandom();
 public AeadKeyRing(String currentId,String current,String previous){
  keys=new KeySet(currentId,current,previous);
  try{for(String id:keys.ids()){byte[] bytes=Base64.getDecoder().decode(keys.value(id));if(bytes.length!=32)throw new IllegalArgumentException();aes.put(id,new SecretKeySpec(bytes,"AES"));}}
  catch(IllegalArgumentException bad){throw new IllegalArgumentException("Configura claves AES independientes, de 32 bytes en base64");}
 }
 public String currentId(){return keys.currentId();}
 public String keyId(String value){return value.startsWith("K1.")?value.split("\\.",3)[1]:"legacy";}
 public boolean needsRotation(String value){return value!=null&&!keyId(value).equals(currentId());}
 /** Predicate for a trusted entity field; identifiers exclude SQL LIKE wildcards. */
 public String oldPredicate(String field){return currentId().equals("legacy")?"("+field+" like 'K1.%' and "+field+" not like 'K1.legacy.%')":"("+field+" is not null and "+field+" not like 'K1."+currentId()+".%')";}
 public String encrypt(byte[] aad,byte[] plaintext){
  try{byte[] nonce=new byte[12];RANDOM.nextBytes(nonce);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,aes.get(currentId()),new GCMParameterSpec(128,nonce));cipher.updateAAD(aad);byte[] encrypted=cipher.doFinal(plaintext);String encoded=Base64.getEncoder().encodeToString(ByteBuffer.allocate(12+encrypted.length).put(nonce).put(encrypted).array());return currentId().equals("legacy")?encoded:"K1."+currentId()+"."+encoded;}
  catch(GeneralSecurityException failure){throw new IllegalStateException("No se pudo proteger el contenido");}
 }
 public byte[] decrypt(byte[] aad,String encrypted){
  try{String id=keyId(encrypted);String encoded=encrypted.startsWith("K1.")?encrypted.split("\\.",3)[2]:encrypted;byte[] value=Base64.getDecoder().decode(encoded);if(value.length<29||!aes.containsKey(id))throw new IllegalArgumentException();Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,aes.get(id),new GCMParameterSpec(128,Arrays.copyOf(value,12)));cipher.updateAAD(aad);return cipher.doFinal(value,12,value.length-12);}
  catch(GeneralSecurityException|RuntimeException failure){throw new IllegalStateException("No se pudo leer el contenido protegido");}
 }
 @Override public String toString(){return "AeadKeyRing[redacted]";}
}
