package bo.edu.uagrm.credenciales.security;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MfaCrypto {
    private static final SecureRandom RANDOM=new SecureRandom();
    private final AeadKeyRing ring;
    public MfaCrypto(String encoded){this(encoded,"legacy","");}
    @org.springframework.beans.factory.annotation.Autowired
    public MfaCrypto(@Value("${app.security.mfa-encryption-key:}") String encoded,@Value("${app.security.mfa-key-id:legacy}") String id,@Value("${app.security.mfa-previous-keys:}") String previous){ring=new AeadKeyRing(id,encoded,previous);}
    public String currentId(){return ring.currentId();}
    public boolean needsRotation(String value){return ring.needsRotation(value);}
    public String keyId(String value){return ring.keyId(value);}
    public String oldPredicate(String field){return ring.oldPredicate(field);}
    public String encrypt(UUID owner,String secret){return ring.encrypt(owner.toString().getBytes(StandardCharsets.UTF_8),secret.getBytes(StandardCharsets.UTF_8));}
    public String decrypt(UUID owner,String encrypted){return new String(ring.decrypt(owner.toString().getBytes(StandardCharsets.UTF_8),encrypted),StandardCharsets.UTF_8);}
    public static String randomToken(){byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    public static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
