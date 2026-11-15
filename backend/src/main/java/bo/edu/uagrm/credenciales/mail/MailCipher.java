package bo.edu.uagrm.credenciales.mail;

import bo.edu.uagrm.credenciales.security.AeadKeyRing;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MailCipher {
 public record Payload(String to,String subject,String body){@Override public String toString(){return "MailPayload[redacted]";}}
 private final AeadKeyRing ring;private final ObjectMapper json;
 public MailCipher(String encoded,ObjectMapper json){this(encoded,json,"legacy","");}
 @org.springframework.beans.factory.annotation.Autowired
 public MailCipher(@Value("${app.mail-outbox.encryption-key:}") String encoded,ObjectMapper json,@Value("${app.mail-outbox.key-id:legacy}") String id,@Value("${app.mail-outbox.previous-keys:}") String previous){ring=new AeadKeyRing(id,encoded,previous);this.json=json;}
 public String currentId(){return ring.currentId();}
 public boolean needsRotation(String value){return ring.needsRotation(value);}
    public String keyId(String value){return ring.keyId(value);}
    public String oldPredicate(String field){return ring.oldPredicate(field);}
 public String encrypt(UUID id,Payload payload){try{return ring.encrypt(aad(id),json.writeValueAsBytes(payload));}catch(Exception failure){throw new IllegalStateException("No se pudo proteger el correo");}}
 public Payload decrypt(UUID id,String value){try{return json.readValue(ring.decrypt(aad(id),value),Payload.class);}catch(Exception failure){throw new IllegalStateException("No se pudo leer el correo protegido");}}
 private byte[] aad(UUID id){return ("mail-outbox:v1:"+id).getBytes(StandardCharsets.UTF_8);}
}
