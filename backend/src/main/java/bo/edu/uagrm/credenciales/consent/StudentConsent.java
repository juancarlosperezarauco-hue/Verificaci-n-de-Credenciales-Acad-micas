package bo.edu.uagrm.credenciales.consent;
import bo.edu.uagrm.credenciales.identity.StudentAccount;
import bo.edu.uagrm.credenciales.tenant.Tenant;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="student_consents")
public class StudentConsent {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="student_id") private StudentAccount student;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="tenant_id") private Tenant tenant;
    @Column(name="token_hash",nullable=false,unique=true,length=64) private String tokenHash;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="used_at") private Instant usedAt;
    @Column(name="revoked_at") private Instant revokedAt;
    @Column(name="used_by") private UUID usedBy;
    protected StudentConsent() {}
    public StudentConsent(StudentAccount s, Tenant t, String hash, Instant now) {
        id=UUID.randomUUID(); student=s; tenant=t; tokenHash=hash; createdAt=now; expiresAt=now.plusSeconds(900);
    }
    public boolean redeemable(Instant now) { return usedAt==null && revokedAt==null && expiresAt.isAfter(now); }
    public void redeem(UUID user, Instant now) { usedBy=user; usedAt=now; }
    public void revoke(Instant now) { revokedAt=now; }
    public UUID getId(){return id;}
    public StudentAccount getStudent(){return student;}
    public Tenant getTenant(){return tenant;}
    public Instant getExpiresAt(){return expiresAt;}
    public Instant getUsedAt(){return usedAt;}
    public Instant getRevokedAt(){return revokedAt;}
}
