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

    public JwtService(@Value("${app.security.jwt-secret}") String secret,
                      @Value("${app.security.jwt-expiration-minutes}") long expirationMinutes) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.expirationMinutes = expirationMinutes;
    }

    public String create(UserPrincipal principal) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer("uagrm-credentials")
                .withSubject(principal.userId().toString())
                .withClaim("tenantId", principal.tenantId() == null ? null : principal.tenantId().toString())
                .withClaim("email", principal.email())
                .withClaim("role", principal.role().name())
                .withClaim("accountType", principal.role() == bo.edu.uagrm.credenciales.user.UserRole.STUDENT ? "STUDENT" : "USER")
                .withIssuedAt(now)
                .withExpiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .sign(algorithm);
    }

    public JwtIdentity verify(String token) {
        DecodedJWT jwt = JWT.require(algorithm).withIssuer("uagrm-credentials").build().verify(token);
        return new JwtIdentity(UUID.fromString(jwt.getSubject()),
                jwt.getClaim("tenantId").asString() == null ? null : UUID.fromString(jwt.getClaim("tenantId").asString()),
                "USER".equals(jwt.getClaim("accountType").asString()));
    }

    public record JwtIdentity(UUID userId, UUID tenantId, boolean userAccount) {}
}

