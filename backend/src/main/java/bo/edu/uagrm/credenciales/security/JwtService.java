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
    private final Algorithm algorithm;
    private final long expirationMinutes;
    private final SessionRegistry sessions;

    public JwtService(@Value("${app.security.jwt-secret}") String secret,
                      @Value("${app.security.jwt-expiration-minutes}") long expirationMinutes) {
        this(secret,expirationMinutes,null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public JwtService(@Value("${app.security.jwt-secret}") String secret,
                      @Value("${app.security.jwt-expiration-minutes}") long expirationMinutes,SessionRegistry sessions) {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32
                || java.util.Set.of("change-this-development-secret-with-at-least-32-characters",
                    "replace-with-a-random-secret-of-at-least-32-characters").contains(secret)) {
            throw new IllegalArgumentException("Configura JWT_SECRET con una clave privada aleatoria de al menos 32 bytes.");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.expirationMinutes = expirationMinutes;
        this.sessions=sessions;
    }

    public String create(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expires=now.plus(expirationMinutes,ChronoUnit.MINUTES);
        if(sessions==null)throw new IllegalStateException("Se requiere el registro de sesiones");
        UUID session=sessions.open(principal,expires);
        return JWT.create()
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
                .sign(algorithm);
    }

    public JwtIdentity verify(String token) {
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

