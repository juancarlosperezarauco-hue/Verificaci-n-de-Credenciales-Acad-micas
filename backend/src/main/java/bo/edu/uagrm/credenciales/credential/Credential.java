package bo.edu.uagrm.credenciales.credential;

import bo.edu.uagrm.credenciales.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credentials")
public class Credential {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    @Column(name = "holder_name", nullable = false, length = 180)
    private String holderName;
    @Column(name = "holder_document", length = 80)
    private String holderDocument;
    @Column(name = "credential_type", nullable = false, length = 60)
    private String credentialType;
    @Column(nullable = false, length = 220)
    private String title;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CredentialStatus status;
    @Column(name = "verification_code", unique = true, length = 40)
    private String verificationCode;
    @Column(name = "document_object_key", length = 500)
    private String documentObjectKey;
    @Column(name = "document_name", length = 255)
    private String documentName;
    @Column(name = "document_content_type", length = 120)
    private String documentContentType;
    @Column(name = "document_hash", length = 64)
    private String documentHash;
    @Column(name = "blockchain_tx_id", length = 180)
    private String blockchainTxId;
    @Column(name = "issued_at") private Instant issuedAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "revocation_reason", length = 500) private String revocationReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Credential() {}

    public Credential(Tenant tenant, String holderName, String holderDocument, String credentialType, String title) {
        this.id = UUID.randomUUID();
        this.tenant = tenant;
        this.holderName = holderName;
        this.holderDocument = holderDocument;
        this.credentialType = credentialType;
        this.title = title;
        this.status = CredentialStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void attachDocument(String objectKey, String name, String contentType, String hash) {
        if (status != CredentialStatus.DRAFT) throw new IllegalStateException("No se puede cambiar el documento de una credencial emitida");
        this.documentObjectKey = objectKey;
        this.documentName = name;
        this.documentContentType = contentType;
        this.documentHash = hash;
        this.updatedAt = Instant.now();
    }

    public void issue(String verificationCode, String blockchainTxId) {
        if (status != CredentialStatus.DRAFT) throw new IllegalStateException("Solo una credencial en borrador puede emitirse");
        if (documentHash == null) throw new IllegalStateException("Debe adjuntar el documento antes de emitir la credencial");
        this.status = CredentialStatus.ISSUED;
        this.verificationCode = verificationCode;
        this.blockchainTxId = blockchainTxId;
        this.issuedAt = Instant.now();
        this.updatedAt = issuedAt;
    }

    public void revoke(String reason) {
        if (status != CredentialStatus.ISSUED) throw new IllegalStateException("Solo una credencial emitida puede revocarse");
        this.status = CredentialStatus.REVOKED;
        this.revocationReason = reason;
        this.revokedAt = Instant.now();
        this.updatedAt = revokedAt;
    }

    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public String getHolderName() { return holderName; }
    public String getHolderDocument() { return holderDocument; }
    public String getCredentialType() { return credentialType; }
    public String getTitle() { return title; }
    public CredentialStatus getStatus() { return status; }
    public String getVerificationCode() { return verificationCode; }
    public String getDocumentName() { return documentName; }
    public String getDocumentHash() { return documentHash; }
    public String getBlockchainTxId() { return blockchainTxId; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public String getRevocationReason() { return revocationReason; }
    public Instant getCreatedAt() { return createdAt; }
}

