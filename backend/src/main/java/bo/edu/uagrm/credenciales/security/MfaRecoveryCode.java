package bo.edu.uagrm.credenciales.security;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="mfa_recovery_codes")
class MfaRecoveryCode {
    @Id UUID id;
    @Column(name="user_id",nullable=false) UUID userId;
    @Column(name="code_hash",nullable=false,unique=true,length=64) String codeHash;
    @Column(nullable=false) boolean used;
    @Column(name="used_at") java.time.Instant usedAt;
    void use(){used=true;usedAt=java.time.Instant.now();}
}
