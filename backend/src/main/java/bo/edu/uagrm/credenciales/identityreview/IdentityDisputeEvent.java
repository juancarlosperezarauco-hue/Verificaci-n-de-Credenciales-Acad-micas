package bo.edu.uagrm.credenciales.identityreview;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="identity_dispute_events")
class IdentityDisputeEvent {
 @Id UUID id;@Column(name="dispute_id",nullable=false) UUID disputeId;@Column(name="actor_id",nullable=false) UUID actorId;
 @Column(nullable=false,length=40) String action;@Column(nullable=false,length=20) String status;@Column(length=2000) String reason;
 @Column(name="occurred_at",nullable=false) Instant occurredAt;protected IdentityDisputeEvent(){}
}
