package bo.edu.uagrm.credenciales.academic;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "academic_import_batches")
class AcademicImportBatch {
    @Id UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "creator_id", nullable = false)
    UUID creatorId;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Column(name = "applied_at")
    Instant appliedAt;

    @Column(nullable = false, columnDefinition = "text")
    String payload;

    @Column(columnDefinition = "text")
    String result;

    protected AcademicImportBatch() {}
}
