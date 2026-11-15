package bo.edu.uagrm.credenciales.workflow;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "request_attachments")
public class RequestAttachment {
    @Id UUID id;

    @Column(name = "request_id", nullable = false)
    UUID requestId;

    @Column(name = "object_key", nullable = false, length = 500)
    String objectKey;

    @Column(name = "content_hash", nullable = false, length = 64)
    String contentHash;

    @Column(name = "content_type", nullable = false, length = 80)
    String contentType;

    @Column(nullable = false, length = 160)
    String name;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected RequestAttachment() {}
}
