package bo.edu.uagrm.credenciales.security;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="login_sessions")
class LoginSession {
    @Id UUID id;
    @Column(name="user_id") UUID userId;
    @Column(name="student_id") UUID studentId;
    @Column(name="tenant_id") UUID tenantId;
    @Column(name="signing_key_id",nullable=false,length=40) String signingKeyId="legacy";
    @Column(name="security_version",nullable=false) long securityVersion;
    @Column(name="mfa_verified",nullable=false) boolean mfaVerified;
    @Column(nullable=false,length=100) String device;
    @Column(name="created_at",nullable=false) Instant createdAt;
    @Column(name="last_seen_at",nullable=false) Instant lastSeenAt;
    @Column(name="expires_at",nullable=false) Instant expiresAt;
    @Column(name="revoked_at") Instant revokedAt;
}
