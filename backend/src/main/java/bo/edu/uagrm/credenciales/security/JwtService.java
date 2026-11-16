package bo.edu.uagrm.credenciales.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final KeySet keys;
    private final java.util.Map<String,Algorithm> algorithms=new java.util.HashMap<>();
    private final long expirationMinutes;
    private final SessionRegistry sessions;

    public JwtService(String secret,long expirationMinutes){this(secret,expirationMinutes,null,"legacy","");}
    public JwtService(String secret,long expirationMinutes,SessionRegistry sessions){this(secret,expirationMinutes,sessions,"legacy","");}
    @org.springframework.beans.factory.annotation.Autowired
    public JwtService(@Value("${app.security.jwt-secret}") String secret,@Value("${app.security.jwt-expiration-minutes}") long expirationMinutes,SessionRegistry sessions,
                      @Value("${app.security.jwt-key-id:legacy}") String id,@Value("${app.security.jwt-previous-keys:}") String previous){
        keys=new KeySet(id,secret,previous);
        for(String key:keys.ids()){
            String value=keys.value(key);
            if(value==null||value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<32||java.util.Set.of("change-this-development-secret-with-at-least-32-characters","replace-with-a-random-secret-of-at-least-32-characters").contains(value))throw new IllegalArgumentException("Configura JWT_SECRET con claves privadas aleatorias de al menos 32 bytes.");
            algorithms.put(key,Algorithm.HMAC256(value));
        }
        this.expirationMinutes=expirationMinutes;this.sessions=sessions;
    }
    public String currentId(){return keys.currentId();}

    public String create(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expires=now.plus(expirationMinutes,ChronoUnit.MINUTES);
        if(sessions==null)throw new IllegalStateException("Se requiere el registro de sesiones");
        UUID session=sessions.open(principal,expires,keys.currentId());
        return JWT.create().withKeyId(keys.currentId())
                .withJWTId(session.toString())
                .withIssuer("uagrm-credentials")
                .withSubject(principal.userId().toString())
                .withClaim("tenantId", principal.tenantId() == null ? null : principal.tenantId().toString())
                .withClaim("email", principal.email())
                .withClaim("role", principal.role().name())
                .withClaim("accountType", principal.role() == bo.edu.uagrm.credenciales.user.UserRole.STUDENT ? "STUDENT" : "USER")
                .withClaim("securityVersion", principal.securityVersion())
                .withIssuedAt(now)
                .withExpiresAt(expires)
                .sign(algorithms.get(keys.currentId()));
    }

    public JwtIdentity verify(String token) {
        String id=JWT.decode(token).getKeyId();if(id==null)id="legacy";
        Algorithm algorithm=algorithms.get(id);if(algorithm==null)throw new com.auth0.jwt.exceptions.JWTVerificationException("Firma de acceso no disponible");
        DecodedJWT jwt = JWT.require(algorithm).withIssuer("uagrm-credentials").build().verify(token);
        return new JwtIdentity(UUID.fromString(jwt.getSubject()),
                jwt.getClaim("tenantId").asString() == null ? null : UUID.fromString(jwt.getClaim("tenantId").asString()),
                "USER".equals(jwt.getClaim("accountType").asString()),
                jwt.getClaim("securityVersion").isMissing() || jwt.getClaim("securityVersion").isNull()
                        ? 0 : jwt.getClaim("securityVersion").asLong(),
                jwt.getId()==null?null:UUID.fromString(jwt.getId()));
    }

    public record JwtIdentity(UUID userId, UUID tenantId, boolean userAccount, long securityVersion,UUID sessionId) {}
}

