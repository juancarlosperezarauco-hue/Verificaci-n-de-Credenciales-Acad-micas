package bo.edu.uagrm.credenciales.mail;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="mail_outbox")
class MailOutbox {
 @Id UUID id;
 @Column(name="student_id",nullable=false) UUID studentId;
 @Column(name="token_id",nullable=false) UUID tokenId;
 @Column(nullable=false,length=30) String kind;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) MailState state;
 @Column(name="encrypted_payload",columnDefinition="text") String encryptedPayload;
 @Column(nullable=false) int attempts;
 @Column(name="created_at",nullable=false) Instant createdAt;
 @Column(name="next_attempt_at",nullable=false) Instant nextAttemptAt;
 @Column(name="expires_at",nullable=false) Instant expiresAt;
 @Column(name="claim_id") UUID claimId;
 @Column(name="lease_until") Instant leaseUntil;
 @Column(name="sent_at") Instant sentAt;
 @Column(name="last_error",length=40) String lastError;
 protected MailOutbox(){}
}
