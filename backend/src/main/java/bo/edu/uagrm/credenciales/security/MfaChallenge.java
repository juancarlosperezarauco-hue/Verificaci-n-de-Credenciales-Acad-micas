package bo.edu.uagrm.credenciales.security;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="mfa_challenges")
class MfaChallenge {
    @Id UUID id;
    @Column(name="user_id",nullable=false) UUID userId;
    @Column(nullable=false) UUID generation;
    @Column(name="token_hash",nullable=false,unique=true,length=64) String tokenHash;
    @Column(name="expires_at",nullable=false) Instant expiresAt;
    @Column(nullable=false) int attempts;
    @Column(nullable=false) boolean consumed;
}
