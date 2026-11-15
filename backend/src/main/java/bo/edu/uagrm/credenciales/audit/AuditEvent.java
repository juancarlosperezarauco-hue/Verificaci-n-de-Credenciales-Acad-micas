package bo.edu.uagrm.credenciales.audit;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity @Immutable @Table(name="audit_events")
class AuditEvent {
 @Id UUID id;
 @Column(name="occurred_at",nullable=false) Instant occurredAt;
 @Column(name="tenant_id") UUID tenantId;
 @Column(name="actor_id") UUID actorId;
 @Column(name="actor_type",nullable=false,length=16) String actorType;
 @Column(name="actor_role",nullable=false,length=40) String actorRole;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=60) AuditAction action;
 @Column(name="resource_type",nullable=false,length=30) String resourceType;
 @Column(name="resource_id",nullable=false) UUID resourceId;
 @Column(name="correlation_id",nullable=false) UUID correlationId;
 @Column(columnDefinition="text") String changes;
 protected AuditEvent(){}
}
