package bo.edu.uagrm.credenciales.storage;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "managed_objects")
public class ManagedObject {
    @Id UUID id;

    @Column(name = "object_key", nullable = false, unique = true, length = 500)
    String objectKey;

    @Column(name = "owner_id", nullable = false)
    UUID ownerId;

    @Column(name = "tenant_id")
    UUID tenantId;

    @Column(nullable = false, length = 30)
    String purpose;

    @Column(name = "content_hash", nullable = false, length = 64)
    String contentHash;

    @Column(name = "size_bytes", nullable = false)
    long sizeBytes;

    @Column(name = "key_id", nullable = false, length = 40)
    String keyId;

    @Column(nullable = false, length = 20)
    String state;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "safe_after", nullable = false)
    Instant safeAfter;

    @Column(name = "delete_requested_at")
    Instant deleteRequestedAt;

    protected ManagedObject() {}
}
