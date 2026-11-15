package bo.edu.uagrm.credenciales.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MfaCrypto {
    private static final SecureRandom RANDOM=new SecureRandom();
    private final SecretKeySpec key;
    public MfaCrypto(@Value("${app.security.mfa-encryption-key:}") String encoded) {
        byte[] bytes;
        try {bytes=Base64.getDecoder().decode(encoded);}catch(IllegalArgumentException e){throw new IllegalArgumentException("MFA_ENCRYPTION_KEY debe ser base64 de 32 bytes");}
        if(bytes.length!=32)throw new IllegalArgumentException("Configura MFA_ENCRYPTION_KEY independiente, de 32 bytes en base64");
        key=new SecretKeySpec(bytes,"AES");
    }
    public String encrypt(UUID owner,String secret) {
        try {
            byte[] nonce=new byte[12];RANDOM.nextBytes(nonce);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,nonce));
            cipher.updateAAD(owner.toString().getBytes(StandardCharsets.UTF_8));
            byte[] encrypted=cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(12+encrypted.length).put(nonce).put(encrypted).array());
        }catch(GeneralSecurityException e){throw new IllegalStateException("No se pudo proteger la configuración MFA");}
    }
    public String decrypt(UUID owner,String encrypted) {
        try {
            byte[] value=Base64.getDecoder().decode(encrypted);
            if(value.length<29)throw new IllegalArgumentException();
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Arrays.copyOf(value,12)));
            cipher.updateAAD(owner.toString().getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(value,12,value.length-12),StandardCharsets.UTF_8);
        }catch(GeneralSecurityException|IllegalArgumentException e){throw new IllegalStateException("No se pudo leer la configuración MFA");}
    }
    public static String randomToken(){byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    public static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
