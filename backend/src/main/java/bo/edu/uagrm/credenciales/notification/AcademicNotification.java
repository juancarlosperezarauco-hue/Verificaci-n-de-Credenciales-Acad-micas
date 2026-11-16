package bo.edu.uagrm.credenciales.notification;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "academic_notifications")
class AcademicNotification {
    @Id UUID id;

    @Column(name = "student_id", nullable = false)
    UUID studentId;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "event_key", nullable = false, length = 160)
    String eventKey;

    @Column(nullable = false, length = 40)
    String kind;

    @Column(name = "resource_id", nullable = false)
    UUID resourceId;

    @Column(nullable = false, length = 180)
    String title;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "read_at")
    Instant readAt;

    protected AcademicNotification() {}
}
