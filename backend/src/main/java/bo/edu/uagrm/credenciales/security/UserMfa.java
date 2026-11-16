package bo.edu.uagrm.credenciales.security;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="user_mfa")
class UserMfa {
    @Id @Column(name="user_id") UUID userId;
    @Column(name="encrypted_secret",length=300) String encryptedSecret;
    @Column(name="pending_secret",length=300) String pendingSecret;
    @Column(nullable=false) boolean enabled;
    @Column(nullable=false) UUID generation=UUID.randomUUID();
    @Column(name="pending_until") Instant pendingUntil;
    @Column(name="last_counter",nullable=false) long lastCounter=-1;
}
