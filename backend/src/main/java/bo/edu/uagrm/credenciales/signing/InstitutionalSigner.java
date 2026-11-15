package bo.edu.uagrm.credenciales.signing;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="institutional_signers")
public class InstitutionalSigner {
 @Id @Column(name="tenant_id") UUID tenantId;@Column(name="encrypted_key",nullable=false,columnDefinition="text") String encryptedKey;
 @Column(name="certificate_der",nullable=false,columnDefinition="text") String certificateDer;
 @Column(nullable=false,length=64) String fingerprint;@Column(nullable=false,length=500) String subject;
 @Column(name="expires_at",nullable=false) Instant expiresAt;@Column(name="test_only",nullable=false) boolean testOnly;
 @Column(nullable=false) boolean required;@Column(name="configured_at",nullable=false) Instant configuredAt;@Version long version;
 protected InstitutionalSigner(){}
}
