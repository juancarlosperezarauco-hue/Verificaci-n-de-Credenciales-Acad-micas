package bo.edu.uagrm.credenciales.identity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_account_tokens")
public class StudentAccountToken {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentAccount student;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 30)
    private StudentTokenType tokenType;
    @Column(name = "pending_email", length = 180) private String pendingEmail;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "used_at") private Instant usedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected StudentAccountToken() {}

    public StudentAccountToken(StudentAccount student, String tokenHash, StudentTokenType tokenType,
                               String pendingEmail, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.student = student;
        this.tokenHash = tokenHash;
        this.tokenType = tokenType;
        this.pendingEmail = pendingEmail;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public StudentAccount getStudent() { return student; }
    public String getPendingEmail() { return pendingEmail; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public void use() { usedAt = Instant.now(); }
}

