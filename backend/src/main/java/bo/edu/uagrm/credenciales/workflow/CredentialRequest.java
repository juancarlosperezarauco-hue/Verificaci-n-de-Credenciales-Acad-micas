package bo.edu.uagrm.credenciales.workflow;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credential_requests")
public class CredentialRequest {
    public enum Status {
        SUBMITTED,
        IN_REVIEW,
        OBSERVED,
        REVIEWED,
        APPROVED,
        FULFILLED,
        REJECTED,
        WITHDRAWN
    }

    @Id UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "student_id", nullable = false)
    UUID studentId;

    @Column(name = "record_id", nullable = false)
    UUID recordId;

    @Column(name = "source_credential_id")
    UUID sourceCredentialId;

    @Column(name = "credential_type", nullable = false, length = 60)
    String credentialType;

    @Column(nullable = false, length = 220)
    String title;

    @Column(length = 2000)
    String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    Status status;

    @Column(name = "reviewer_id")
    UUID reviewerId;

    @Column(name = "approver_id")
    UUID approverId;

    @Column(name = "template_version_id")
    UUID templateVersionId;

    @Column(name = "student_revision", nullable = false)
    long studentRevision;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Version long version;

    protected CredentialRequest() {}
}
