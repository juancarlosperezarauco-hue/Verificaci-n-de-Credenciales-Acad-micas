package bo.edu.uagrm.credenciales.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.sql.Timestamp;
import java.time.*;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AuthRateLimiter {
    private final JdbcTemplate jdbc;private final byte[] key;private final Clock clock;private final boolean enabled;
    public AuthRateLimiter(JdbcTemplate jdbc,Clock clock,@Value("${app.security.jwt-secret}") String secret,
                           @Value("${app.security.rate-limit-enabled:true}") boolean enabled){this.jdbc=jdbc;this.clock=clock;this.key=secret.getBytes(StandardCharsets.UTF_8);this.enabled=enabled;}
    public boolean enabled(){return enabled;}
    public record Decision(boolean allowed,long retryAfter){}
    /** JDBC statements run in auto-commit before controller transactions; update/increment is atomic across replicas. */
    public Decision take(String category,String identity,int limit,int seconds){
        String bucket=hash(category+":"+identity);Instant now=clock.instant();Timestamp time=Timestamp.from(now),expiry=Timestamp.from(now.plusSeconds(seconds));
        int changed=jdbc.update("update auth_rate_limits set attempts=case when expires_at<=? then 1 else attempts+1 end, expires_at=case when expires_at<=? then ? else expires_at end where bucket_key=?",time,time,expiry,bucket);
        if(changed==0){try{jdbc.update("insert into auth_rate_limits(bucket_key,attempts,expires_at) values(?,1,?)",bucket,expiry);}catch(DuplicateKeyException race){jdbc.update("update auth_rate_limits set attempts=attempts+1 where bucket_key=?",bucket);}}
        return jdbc.queryForObject("select attempts,expires_at from auth_rate_limits where bucket_key=?",(rs,n)->new Decision(rs.getInt(1)<=limit,Math.max(1,Duration.between(now,rs.getTimestamp(2).toInstant()).getSeconds()+1)),bucket);
    }
    private String hash(String value){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(("auth-limit-v1:"+value).getBytes(StandardCharsets.UTF_8)));}catch(GeneralSecurityException e){throw new IllegalStateException(e);}}
    @Scheduled(fixedDelay=3600000)
    public void cleanExpired(){if(enabled)jdbc.update("delete from auth_rate_limits where expires_at<?",Timestamp.from(clock.instant().minusSeconds(86400)));}
}
