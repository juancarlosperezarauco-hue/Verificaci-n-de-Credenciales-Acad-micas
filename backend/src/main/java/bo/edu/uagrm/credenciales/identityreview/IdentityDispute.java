package bo.edu.uagrm.credenciales.identityreview;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "identity_disputes")
public class IdentityDispute {
    public enum Status {
        OPEN,
        OBSERVED,
        RESOLVED,
        REJECTED,
        WITHDRAWN
    }

    @Id UUID id;

    @Column(name = "student_id", nullable = false)
    UUID studentId;

    @Column(nullable = false, length = 2)
    String country;

    @Column(name = "document_type", nullable = false, length = 30)
    String documentType;

    @Column(name = "document_number", nullable = false, length = 80)
    String documentNumber;

    @Column(nullable = false, length = 2000)
    String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    Status status;

    @Column(length = 2000)
    String resolution;

    @Column(name = "front_key", length = 500)
    String frontKey;

    @Column(name = "back_key", length = 500)
    String backKey;

    @Column(name = "front_hash", nullable = false, length = 64)
    String frontHash;

    @Column(name = "back_hash", nullable = false, length = 64)
    String backHash;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "resolved_at")
    Instant resolvedAt;

    @Column(name = "resolver_id")
    UUID resolverId;

    @Column(name = "files_purged_at")
    Instant filesPurgedAt;

    @Column(name = "retired_at")
    Instant retiredAt;

    @Version long version;

    protected IdentityDispute() {}
}
