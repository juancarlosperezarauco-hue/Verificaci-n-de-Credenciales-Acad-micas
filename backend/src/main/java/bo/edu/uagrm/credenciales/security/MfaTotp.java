package bo.edu.uagrm.credenciales.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Clock;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class MfaTotp {
    private static final String ALPHABET="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private final Clock clock;
    public MfaTotp(Clock clock){this.clock=clock;}
    public long counter(){return clock.instant().getEpochSecond()/30;}
    public String secret(){byte[] value=new byte[20];new SecureRandom().nextBytes(value);return encode(value);}
    public long match(String secret,String value,long lastCounter) {
        if(value==null||!value.matches("[0-9]{6}"))return -1;
        long now=counter();byte[] expected=value.getBytes(StandardCharsets.US_ASCII);
        for(long candidate=now-1;candidate<=now+1;candidate++)if(candidate>lastCounter&&candidate>=0
                &&MessageDigest.isEqual(code(decode(secret),candidate,6).getBytes(StandardCharsets.US_ASCII),expected))return candidate;
        return -1;
    }
    public String code(byte[] secret,long counter,int digits) {
        if(digits!=6&&digits!=8)throw new IllegalArgumentException("Longitud OTP no admitida");
        try {
            Mac mac=Mac.getInstance("HmacSHA1");mac.init(new SecretKeySpec(secret,"HmacSHA1"));
            byte[] digest=mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());int offset=digest[digest.length-1]&15;
            int binary=((digest[offset]&127)<<24)|((digest[offset+1]&255)<<16)|((digest[offset+2]&255)<<8)|(digest[offset+3]&255);
            return String.format(Locale.ROOT,"%0"+digits+"d",binary%(digits==6?1_000_000:100_000_000));
        }catch(GeneralSecurityException e){throw new IllegalStateException("OTP no disponible",e);}
    }
    public static String encode(byte[] input){StringBuilder out=new StringBuilder();int bits=0,buffer=0;for(byte b:input){buffer=(buffer<<8)|(b&255);bits+=8;while(bits>=5){bits-=5;out.append(ALPHABET.charAt((buffer>>bits)&31));}}if(bits>0)out.append(ALPHABET.charAt((buffer<<(5-bits))&31));return out.toString();}
    public static byte[] decode(String input){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();int bits=0,buffer=0;for(char c:input.toCharArray()){int value=ALPHABET.indexOf(c);if(value<0)throw new IllegalArgumentException("Semilla OTP inválida");buffer=(buffer<<5)|value;bits+=5;if(bits>=8){bits-=8;out.write((buffer>>bits)&255);}}return out.toByteArray();}
}
