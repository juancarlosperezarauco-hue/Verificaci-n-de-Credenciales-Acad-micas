package bo.edu.uagrm.credenciales.identityreview;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="identity_review_submissions")
public class IdentitySubmission {
    public enum Status { PENDING, VERIFIED, REJECTED }
    @Id UUID id;
    @Column(name="student_id",nullable=false) UUID studentId;
    @Column(name="tenant_id",nullable=false) UUID tenantId;
    @Column(name="consent_id",nullable=false) UUID consentId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) Status status;
    @Column(name="profile_json",nullable=false,columnDefinition="text") String profileJson;
    @Column(name="profile_hash",nullable=false,length=64) String profileHash;
    @Column(name="front_key",length=300) String frontKey;
    @Column(name="front_hash",nullable=false,length=64) String frontHash;
    @Column(name="back_key",length=300) String backKey;
    @Column(name="back_hash",nullable=false,length=64) String backHash;
    @Column(name="photo_key",length=300) String photoKey;
    @Column(name="photo_hash",length=64) String photoHash;
    @Column(name="submitted_at",nullable=false) Instant submittedAt;
    @Column(name="accepted_at",nullable=false) Instant acceptedAt;
    @Column(name="reviewed_at") Instant reviewedAt;
    @Column(name="reviewer_id") UUID reviewerId;
    @Column(length=1000) String reason;
    @Column(name="withdrawn_at") Instant withdrawnAt;
    @Column(name="retired_at") Instant retiredAt;
    @Column(name="files_purged_at") Instant filesPurgedAt;
    @Version long version;
    protected IdentitySubmission() {}
}
