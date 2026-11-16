package bo.edu.uagrm.credenciales.credential;

import bo.edu.uagrm.credenciales.student.Student;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_account_id")
    private bo.edu.uagrm.credenciales.identity.StudentAccount studentAccount;

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

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revocation_reason", length = 500)
    private String revocationReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "source_request_id", unique = true)
    private UUID sourceRequestId;

    @Column(name = "template_version_id")
    private UUID templateVersionId;

    @Column(name = "signer_fingerprint", length = 64)
    private String signerFingerprint;

    @Column(name = "signing_certificate", columnDefinition = "text")
    private String signingCertificate;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "signature_test_only", nullable = false)
    private boolean signatureTestOnly;

    @Column(name = "issue_operation_key", length = 100)
    private String issueOperationKey;

    @Column(name = "replaces_id")
    private UUID replacesId;

    @Column(name = "replaced_by_id")
    private UUID replacedById;

    protected Credential() {}

    public Credential(
            Tenant tenant,
            bo.edu.uagrm.credenciales.identity.StudentAccount student,
            String credentialType,
            String title) {
        this.id = UUID.randomUUID();
        this.tenant = tenant;
        this.studentAccount = student;
        this.holderName = student.getFullName();
        this.holderDocument = student.getDocumentNumber();
        this.credentialType = credentialType;
        this.title = title;
        this.status = CredentialStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void attachDocument(String objectKey, String name, String contentType, String hash) {
        if (status != CredentialStatus.DRAFT)
            throw new IllegalStateException(
                    "No se puede cambiar el documento de una credencial emitida");
        this.documentObjectKey = objectKey;
        this.documentName = name;
        this.documentContentType = contentType;
        this.documentHash = hash;
        this.signerFingerprint = null;
        this.signingCertificate = null;
        this.signedAt = null;
        this.signatureTestOnly = false;
        this.updatedAt = Instant.now();
    }

    public void prepareVerificationCode(String verificationCode) {
        if (status != CredentialStatus.DRAFT) {
            throw new IllegalStateException(
                    "Solo una credencial en borrador puede preparar su verificación");
        }
        if (this.verificationCode == null) this.verificationCode = verificationCode;
    }

    public void issue(String verificationCode, String blockchainTxId) {
        if (status != CredentialStatus.DRAFT)
            throw new IllegalStateException("Solo una credencial en borrador puede emitirse");
        if (documentHash == null)
            throw new IllegalStateException(
                    "Debe adjuntar el documento antes de emitir la credencial");
        this.status = CredentialStatus.ISSUED;
        if (this.verificationCode == null) this.verificationCode = verificationCode;
        this.blockchainTxId = blockchainTxId;
        this.issuedAt = Instant.now();
        this.updatedAt = issuedAt;
    }

    public void revoke(String reason) {
        if (status != CredentialStatus.ISSUED)
            throw new IllegalStateException("Solo una credencial emitida puede revocarse");
        this.status = CredentialStatus.REVOKED;
        this.revocationReason = reason;
        this.revokedAt = Instant.now();
        this.updatedAt = revokedAt;
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Student getStudent() {
        return student;
    }

    public bo.edu.uagrm.credenciales.identity.StudentAccount getStudentAccount() {
        return studentAccount;
    }

    public String getHolderName() {
        return holderName;
    }

    public String getHolderDocument() {
        return holderDocument;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public String getTitle() {
        return title;
    }

    public CredentialStatus getStatus() {
        return status;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public String getDocumentName() {
        return documentName;
    }

    public String getDocumentObjectKey() {
        return documentObjectKey;
    }

    public String getDocumentContentType() {
        return documentContentType;
    }

    public String getDocumentHash() {
        return documentHash;
    }

    public String getBlockchainTxId() {
        return blockchainTxId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public UUID getSourceRequestId() {
        return sourceRequestId;
    }

    public UUID getTemplateVersionId() {
        return templateVersionId;
    }

    public String getSignerFingerprint() {
        return signerFingerprint;
    }

    public String getSigningCertificate() {
        return signingCertificate;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public boolean isSignatureTestOnly() {
        return signatureTestOnly;
    }

    public String getIssueOperationKey() {
        return issueOperationKey;
    }

    public UUID getReplacesId() {
        return replacesId;
    }

    public UUID getReplacedById() {
        return replacedById;
    }

    public void configureRequest(UUID request, UUID template) {
        if (status != CredentialStatus.DRAFT) throw new IllegalStateException("Solo borradores.");
        sourceRequestId = request;
        templateVersionId = template;
    }

    public void selectTemplate(UUID template) {
        if (status != CredentialStatus.DRAFT) throw new IllegalStateException("Solo borradores.");
        if (sourceRequestId != null && !java.util.Objects.equals(templateVersionId, template))
            throw new IllegalArgumentException("Usa la plantilla aprobada en la solicitud.");
        templateVersionId = template;
    }

    public void signed(String fingerprint, String certificate, Instant at, boolean test) {
        if (status != CredentialStatus.DRAFT) throw new IllegalStateException("Solo borradores.");
        signerFingerprint = fingerprint;
        signingCertificate = certificate;
        signedAt = at;
        signatureTestOnly = test;
    }

    public void operationKey(String key) {
        issueOperationKey = key;
    }

    public void replaces(UUID oldId) {
        replacesId = oldId;
    }

    public void replacedBy(UUID newId) {
        replacedById = newId;
    }
}
