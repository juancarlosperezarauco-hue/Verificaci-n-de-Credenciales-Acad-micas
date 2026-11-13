package bo.edu.uagrm.credenciales.platform;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="organization_status_events")
public class OrganizationStatusEvent {
 @Id private UUID id;
 @Column(name="tenant_id",nullable=false) private UUID tenantId;
 @Column(name="actor_id",nullable=false) private UUID actorId;
 @Enumerated(EnumType.STRING) @Column(name="previous_status",nullable=false,length=30) private OrganizationStatus previousStatus;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private OrganizationStatus status;
 @Column(nullable=false,length=500) private String reason;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 protected OrganizationStatusEvent(){}
 public OrganizationStatusEvent(UUID tenant,UUID actor,OrganizationStatus previous,OrganizationStatus next,String reason){id=UUID.randomUUID();tenantId=tenant;actorId=actor;previousStatus=previous;status=next;this.reason=reason;createdAt=Instant.now();}
 public UUID getId(){return id;} public UUID getActorId(){return actorId;}
 public OrganizationStatus getPreviousStatus(){return previousStatus;} public OrganizationStatus getStatus(){return status;}
 public String getReason(){return reason;} public Instant getCreatedAt(){return createdAt;}
}
