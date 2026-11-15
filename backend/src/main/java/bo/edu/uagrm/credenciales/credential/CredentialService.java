package bo.edu.uagrm.credenciales.credential;

import static bo.edu.uagrm.credenciales.audit.AuditAction.*;
import static bo.edu.uagrm.credenciales.audit.AuditTrail.Resource.CREDENTIAL;

import bo.edu.uagrm.credenciales.audit.AuditTrail;
import bo.edu.uagrm.credenciales.blockchain.BlockchainAnchorPort;
import bo.edu.uagrm.credenciales.consent.ConsentService;
import bo.edu.uagrm.credenciales.signing.SigningService;
import bo.edu.uagrm.credenciales.template.TemplateService;
import bo.edu.uagrm.credenciales.tenant.Tenant;
import bo.edu.uagrm.credenciales.tenant.TenantRepository;
import bo.edu.uagrm.credenciales.workflow.RequestIssuancePolicy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Coordinates credential commands; document I/O and verification have their own boundaries. */
@Service
public class CredentialService {
    private final CredentialRepository credentials;
    private final TenantRepository tenants;
    private final ConsentService consents;
    private final BlockchainAnchorPort blockchain;
    private final CredentialDocumentService documents;
    private final CredentialVerificationService verification;
    private final CredentialPdfService pdf;
    private final AuditTrail audit;
    private final TemplateService templates;
    private final SigningService signing;
    private final RequestIssuancePolicy requests;
    private final Clock clock;

    public CredentialService(
            CredentialRepository credentials,
            TenantRepository tenants,
            ConsentService consents,
            BlockchainAnchorPort blockchain,
            CredentialDocumentService documents,
            CredentialVerificationService verification,
            CredentialPdfService pdf,
            AuditTrail audit,
            TemplateService templates,
            SigningService signing,
            RequestIssuancePolicy requests,
            Clock clock) {
        this.credentials = credentials;
        this.tenants = tenants;
        this.consents = consents;
        this.blockchain = blockchain;
        this.documents = documents;
        this.verification = verification;
        this.pdf = pdf;
        this.audit = audit;
        this.templates = templates;
        this.signing = signing;
        this.requests = requests;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CredentialDtos.Response> list(UUID tenantId) {
        return credentials.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(CredentialDtos.Response::from)
                .toList();
    }

    @Transactional
    public CredentialDtos.Response create(UUID tenantId, CredentialDtos.CreateRequest request) {
        return createDraft(
                tenant(tenantId), request.studentId(), request.credentialType(), request.title());
    }

    @Transactional
    public List<CredentialDtos.Response> createBatch(
            UUID tenantId, CredentialDtos.CreateBatchRequest request) {
        Tenant tenant = tenant(tenantId);
        return request.studentIds().stream()
                .distinct()
                .map(student ->
                        createDraft(tenant, student, request.credentialType(), request.title()))
                .toList();
    }

    private CredentialDtos.Response createDraft(
            Tenant tenant, UUID studentId, String type, String title) {
        var student = consents.authorizedStudent(tenant.getId(), studentId);
        Credential credential = new Credential(tenant, student, type.trim(), title.trim());
        credentials.save(credential);
        audit.record(CREDENTIAL_CREATED, tenant.getId(), CREDENTIAL, credential.getId());
        return CredentialDtos.Response.from(credential);
    }

    @Transactional
    public CredentialDtos.Response attachDocument(UUID tenantId, UUID id, MultipartFile file) {
        Credential credential = findOwnedForUpdate(id, tenantId);
        requireDraft(credential, "No se puede cambiar el documento de una credencial emitida");
        if (file.isEmpty()) throw new IllegalArgumentException("El documento está vacío");
        requests.approved(credential);
        try {
            String name =
                    file.getOriginalFilename() == null
                            ? "documento.pdf"
                            : file.getOriginalFilename();
            documents.attach(credential, file.getBytes(), name);
            audit.record(DOCUMENT_ATTACHED, tenantId, CREDENTIAL, id);
            return CredentialDtos.Response.from(credential);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el documento", exception);
        }
    }

    @Transactional
    public CredentialDtos.Response issue(UUID tenantId, UUID id) {
        return issue(tenantId, id, null);
    }

    @Transactional
    public CredentialDtos.Response issue(UUID tenantId, UUID id, String operationKey) {
        Credential credential = findOwnedForUpdate(id, tenantId);
        if (operationKey != null && !operationKey.matches("[A-Za-z0-9_-]{16,100}")) {
            throw new IllegalArgumentException("Clave de idempotencia inválida.");
        }
        if (operationKey != null
                && operationKey.equals(credential.getIssueOperationKey())
                && credential.getStatus() != CredentialStatus.DRAFT) {
            return CredentialDtos.Response.from(credential);
        }
        requireDraft(credential, "Solo una credencial en borrador puede emitirse");
        if (credential.getStudentAccount() != null) {
            consents.authorizedStudent(tenantId, credential.getStudentAccount().getId());
        }
        if (credential.getDocumentHash() == null) {
            throw new IllegalStateException(
                    "Debe adjuntar el documento antes de emitir la credencial");
        }
        requests.approved(credential);
        validateSignature(credential, documents.readValidated(credential));

        String code =
                credential.getVerificationCode() == null
                        ? uniqueVerificationCode()
                        : credential.getVerificationCode();
        String transactionId = blockchain.anchor(tenantId, id, credential.getDocumentHash());
        credential.issue(code, transactionId);
        credential.operationKey(operationKey);
        requests.fulfilled(credential);
        audit.record(CREDENTIAL_ISSUED, tenantId, CREDENTIAL, id);
        return CredentialDtos.Response.from(credential);
    }

    private void validateSignature(Credential credential, byte[] content) {
        if (signing.status(credential.getTenant().getId()).required()
                && credential.getSignedAt() == null) {
            throw new IllegalArgumentException(
                    "La institución exige firma digital antes de emitir.");
        }
        if (credential.getSignedAt() == null) return;
        var check = signing.verify(content, credential.getSignerFingerprint());
        if (!check.cryptographicValid()
                || !check.coversWholeDocument()
                || !check.signerMatches()
                || !check.certificateCurrentlyValid()) {
            throw new IllegalArgumentException(
                    "La firma del documento no es válida o el certificado está vencido.");
        }
    }

    @Transactional
    public CredentialDtos.Response generateDocument(UUID tenantId, UUID id) {
        return generateDocument(tenantId, id, null);
    }

    @Transactional
    public CredentialDtos.Response generateDocument(UUID tenantId, UUID id, UUID templateId) {
        Credential credential = findOwnedForUpdate(id, tenantId);
        requireDraft(credential, "Solo se puede generar el documento de un borrador");
        requests.approved(credential);
        if (templateId != null) {
            templates.require(tenantId, templateId, credential.getCredentialType(), true);
            credential.selectTemplate(templateId);
        }
        if (credential.getVerificationCode() == null) {
            credential.prepareVerificationCode(uniqueVerificationCode());
        }
        var template =
                credential.getTemplateVersionId() == null
                        ? null
                        : templates.require(
                                tenantId,
                                credential.getTemplateVersionId(),
                                credential.getCredentialType(),
                                false);
        byte[] content =
                pdf.generate(credential, template, requests.programName(credential), false);
        String name =
                credential.getCredentialType().toLowerCase(Locale.ROOT)
                        + "-"
                        + credential.getHolderName().replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ]+", "-")
                        + ".pdf";
        documents.attach(credential, content, name);
        audit.record(DOCUMENT_GENERATED, tenantId, CREDENTIAL, id);
        return CredentialDtos.Response.from(credential);
    }

    @Transactional
    public CredentialDtos.Response sign(UUID tenantId, UUID id) {
        Credential credential = findOwnedForUpdate(id, tenantId);
        requireDraft(credential, "Solo se firman borradores.");
        requests.approved(credential);
        var signed = signing.sign(tenantId, documents.readValidated(credential));
        documents.attach(credential, signed.bytes(), credential.getDocumentName());
        credential.signed(
                signed.fingerprint(), signed.certificate(), clock.instant(), signed.testOnly());
        audit.record(CREDENTIAL_SIGNED, tenantId, CREDENTIAL, id);
        return CredentialDtos.Response.from(credential);
    }

    @Transactional(readOnly = true)
    public CredentialDtos.StoredDocument document(UUID tenantId, UUID id) {
        Credential credential = findOwned(id, tenantId);
        if (credential.getDocumentObjectKey() == null) {
            throw new IllegalArgumentException("La credencial no tiene un documento adjunto");
        }
        return new CredentialDtos.StoredDocument(
                documents.read(credential),
                credential.getDocumentContentType(),
                credential.getDocumentName());
    }

    @Transactional
    public CredentialDtos.Response revoke(UUID tenantId, UUID id, String reason) {
        Credential credential = findOwnedForUpdate(id, tenantId);
        credential.revoke(reason.trim());
        audit.record(CREDENTIAL_REVOKED, tenantId, CREDENTIAL, id);
        return CredentialDtos.Response.from(credential);
    }

    @Transactional(readOnly = true)
    public CredentialDtos.PublicVerification verify(String code) {
        return verification.verify(code);
    }

    @Transactional(readOnly = true)
    public List<CredentialDtos.PublicVerification> personalHistory(UUID studentId) {
        return verification.personalHistory(studentId);
    }

    @Transactional(readOnly = true)
    public CredentialDtos.StoredDocument personalDocument(UUID studentId, UUID id) {
        return verification.personalDocument(studentId, id);
    }

    @Transactional(readOnly = true)
    public CredentialDtos.DocumentCheck checkDocument(String code, MultipartFile file) {
        return verification.checkDocument(code, file);
    }

    private Tenant tenant(UUID id) {
        return tenants.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organización no encontrada"));
    }

    private Credential findOwned(UUID id, UUID tenantId) {
        return credentials
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Credencial no encontrada"));
    }

    private Credential findOwnedForUpdate(UUID id, UUID tenantId) {
        // Every credential mutation keeps the same student -> credential lock order.
        requests.lockOwner(tenantId, id);
        return credentials
                .findLockedByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Credencial no encontrada"));
    }

    private void requireDraft(Credential credential, String message) {
        if (credential.getStatus() != CredentialStatus.DRAFT)
            throw new IllegalStateException(message);
    }

    private String uniqueVerificationCode() {
        String code;
        do {
            code = "CRED-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        } while (credentials.existsByVerificationCode(code));
        return code;
    }
}
