package bo.edu.uagrm.credenciales.security;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="auth_rate_limits")
class AuthRateBucket {
    @Id @Column(name="bucket_key",length=64) String key;
    @Column(nullable=false) int attempts;
    @Column(name="expires_at",nullable=false) Instant expiresAt;
}
