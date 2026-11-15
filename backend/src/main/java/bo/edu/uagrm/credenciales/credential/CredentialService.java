package bo.edu.uagrm.credenciales.credential;

import static bo.edu.uagrm.credenciales.audit.AuditAction.*;
import static bo.edu.uagrm.credenciales.audit.AuditTrail.Resource.*;

import bo.edu.uagrm.credenciales.audit.*;
import bo.edu.uagrm.credenciales.blockchain.BlockchainAnchorPort;
import bo.edu.uagrm.credenciales.tenant.Tenant;
import bo.edu.uagrm.credenciales.tenant.TenantRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class CredentialService {
    private final AuditTrail audit;
    private final CredentialRepository credentialRepository;
    private final TenantRepository tenantRepository;
    private final bo.edu.uagrm.credenciales.consent.ConsentService consents;
    private final BlockchainAnchorPort blockchain;
    private final bo.edu.uagrm.credenciales.storage.ManagedStorage storage;
    private final bo.edu.uagrm.credenciales.template.TemplateService templates;
    private final bo.edu.uagrm.credenciales.signing.SigningService signing;
    private final bo.edu.uagrm.credenciales.workflow.RequestIssuancePolicy requests;
    private final PdfSafety safety;
    private final bo.edu.uagrm.credenciales.platform.GovernanceRepository governance;
    private final CredentialPdfService pdfService;

    public CredentialService(
            CredentialRepository credentialRepository,
            TenantRepository tenantRepository,
            bo.edu.uagrm.credenciales.consent.ConsentService consents,
            BlockchainAnchorPort blockchain,
            bo.edu.uagrm.credenciales.storage.ManagedStorage storage,
            CredentialPdfService pdfService,
            AuditTrail audit,
            bo.edu.uagrm.credenciales.template.TemplateService templates,
            bo.edu.uagrm.credenciales.signing.SigningService signing,
            bo.edu.uagrm.credenciales.workflow.RequestIssuancePolicy requests,
            PdfSafety safety,
            bo.edu.uagrm.credenciales.platform.GovernanceRepository governance) {
        this.audit = audit;
        this.credentialRepository = credentialRepository;
        this.tenantRepository = tenantRepository;
        this.consents = consents;
        this.blockchain = blockchain;
        this.storage = storage;
        this.pdfService = pdfService;
        this.templates = templates;
        this.signing = signing;
        this.requests = requests;
        this.safety = safety;
        this.governance = governance;
    }

    @Transactional(readOnly = true)
    public List<CredentialDtos.Response> list(UUID tenantId) {
        return credentialRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(CredentialDtos.Response::from)
                .toList();
    }

    @Transactional
    public CredentialDtos.Response create(UUID tenantId, CredentialDtos.CreateRequest request) {
        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Organización no encontrada"));
        var student = consents.authorizedStudent(tenantId, request.studentId());
        Credential credential =
                new Credential(
                        tenant, student, request.credentialType().trim(), request.title().trim());
        credentialRepository.save(credential);
        audit.record(CREDENTIAL_CREATED, tenantId, CREDENTIAL, credential.getId());
        return CredentialDtos.Response.from(credential);
    }

    @Transactional
    public List<CredentialDtos.Response> createBatch(
            UUID tenantId, CredentialDtos.CreateBatchRequest request) {
        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Organización no encontrada"));
        return request.studentIds().stream()
                .distinct()
                .map(studentId -> {
                    var student = consents.authorizedStudent(tenantId, studentId);
                    Credential credential =
                            new Credential(
                                    tenant,
                                    student,
                                    request.credentialType().trim(),
                                    request.title().trim());
                    credentialRepository.save(credential);
                    audit.record(CREDENTIAL_CREATED, tenantId, CREDENTIAL, credential.getId());
                    return CredentialDtos.Response.from(credential);
                })
                .toList();
    }

    @Transactional
    public CredentialDtos.Response attachDocument(UUID tenantId, UUID id, MultipartFile file) {
        Credential credential = findOwnedForUpdate(id, tenantId);
        if (credential.getStatus() != CredentialStatus.DRAFT) {
            throw new IllegalStateException(
                    "No se puede cambiar el documento de una credencial emitida");
        }
        if (file.isEmpty()) throw new IllegalArgumentException("El documento está vacío");
        try {
            byte[] content = file.getBytes();
            safety.validate(content);
            requests.approved(credential);
            String originalName =
                    file.getOriginalFilename() == null
                            ? "documento.pdf"
                            : file.getOriginalFilename();
            String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            // Object storage is not rolled back with PostgreSQL. Never overwrite a committed
            // object.
            String objectKey =
                    tenantId + "/" + id + "/uploads/" + UUID.randomUUID() + "/" + safeName;
            String contentType =
                    file.getContentType() == null
                            ? "application/octet-stream"
                            : file.getContentType();
            String storedKey =
                    storage.store(tenantId, id, "CREDENTIAL", content, "application/pdf");
            contentType = "application/pdf";
            credential.attachDocument(storedKey, originalName, contentType, sha256(content));
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
        if (operationKey != null && !operationKey.matches("[A-Za-z0-9_-]{16,100}"))
            throw new IllegalArgumentException("Clave de idempotencia inválida.");
        if (operationKey != null
                && operationKey.equals(credential.getIssueOperationKey())
                && credential.getStatus() != CredentialStatus.DRAFT)
            return CredentialDtos.Response.from(credential);
        if (credential.getStatus() != CredentialStatus.DRAFT) {
            throw new IllegalStateException("Solo una credencial en borrador puede emitirse");
        }
        if (credential.getStudentAccount() != null) {
            consents.authorizedStudent(tenantId, credential.getStudentAccount().getId());
        }
        String code =
                credential.getVerificationCode() == null
                        ? uniqueVerificationCode()
                        : credential.getVerificationCode();
        if (credential.getDocumentHash() == null) {
            throw new IllegalStateException(
                    "Debe adjuntar el documento antes de emitir la credencial");
        }
        requests.approved(credential);
        byte[] bytes = checkedDocument(credential);
        safety.validate(bytes);
        if (signing.status(tenantId).required() && credential.getSignedAt() == null)
            throw new IllegalArgumentException(
                    "La institución exige firma digital antes de emitir.");
        if (credential.getSignedAt() != null) {
            var check = signing.verify(bytes, credential.getSignerFingerprint());
            if (!check.cryptographicValid()
                    || !check.coversWholeDocument()
                    || !check.signerMatches()
                    || !check.certificateCurrentlyValid())
                throw new IllegalArgumentException(
                        "La firma del documento no es válida o el certificado está vencido.");
        }
        String transactionId = blockchain.anchor(tenantId, id, credential.getDocumentHash());
        credential.issue(code, transactionId);
        credential.operationKey(operationKey);
        requests.fulfilled(credential);
        audit.record(CREDENTIAL_ISSUED, tenantId, CREDENTIAL, id);
        return CredentialDtos.Response.from(credential);
    }

    @Transactional
    public CredentialDtos.Response generateDocument(UUID tenantId, UUID id) {
        return generateDocument(tenantId, id, null);
    }

    @Transactional
    public CredentialDtos.Response generateDocument(UUID tenantId, UUID id, UUID templateId) {
        Credential credential = findOwnedForUpdate(id, tenantId);
        if (credential.getStatus() != CredentialStatus.DRAFT) {
            throw new IllegalStateException("Solo se puede generar el documento de un borrador");
        }
        requests.approved(credential);
        if (credential.getSourceRequestId() != null
                && templateId != null
                && !templateId.equals(credential.getTemplateVersionId()))
            throw new IllegalArgumentException("La plantilla fue fijada en la solicitud aprobada.");
        if (templateId != null) {
            templates.require(tenantId, templateId, credential.getCredentialType(), true);
            credential.selectTemplate(templateId);
        }
        if (credential.getVerificationCode() == null)
            credential.prepareVerificationCode(uniqueVerificationCode());
        byte[] content =
                pdfService.generate(
                        credential,
                        credential.getTemplateVersionId() == null
                                ? null
                                : templates.require(
                                        tenantId,
                                        credential.getTemplateVersionId(),
                                        credential.getCredentialType(),
                                        false),
                        requests.programName(credential),
                        false);
        safety.validate(content);
        String fileName =
                credential.getCredentialType().toLowerCase()
                        + "-"
                        + credential.getHolderName().replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ]+", "-")
                        + ".pdf";
        String objectKey = tenantId + "/" + id + "/generated/" + UUID.randomUUID() + "/" + fileName;
        objectKey = storage.store(tenantId, id, "CREDENTIAL", content, "application/pdf");
        credential.attachDocument(objectKey, fileName, "application/pdf", sha256(content));
        audit.record(DOCUMENT_GENERATED, tenantId, CREDENTIAL, id);
        return CredentialDtos.Response.from(credential);
    }

    @Transactional(readOnly = true)
    public CredentialDtos.StoredDocument document(UUID tenantId, UUID id) {
        Credential credential = findOwned(id, tenantId);
        if (credential.getDocumentObjectKey() == null) {
            throw new IllegalArgumentException("La credencial no tiene un documento adjunto");
        }
        return new CredentialDtos.StoredDocument(
                checkedDocument(credential),
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
        return credentialRepository
                .findByVerificationCode(code.trim().toUpperCase())
                .filter(credential -> credential.getStatus() != CredentialStatus.DRAFT)
                .map(this::publicView)
                .orElseThrow(() -> new IllegalArgumentException("Credencial no encontrada"));
    }

    private Credential findOwned(UUID id, UUID tenantId) {
        return credentialRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Credencial no encontrada"));
    }

    private Credential findOwnedForUpdate(UUID id, UUID tenantId) {
        requests.lockOwner(tenantId, id);
        return credentialRepository
                .findLockedByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Credencial no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<CredentialDtos.PublicVerification> personalHistory(UUID studentId) {
        return credentialRepository
                .findAllByStudentAccountIdOrderByCreatedAtDesc(studentId)
                .stream()
                .filter(c -> c.getStatus() != CredentialStatus.DRAFT)
                .map(this::publicView)
                .toList();
    }

    private String uniqueVerificationCode() {
        String code;
        do {
            code = "CRED-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        } while (credentialRepository.existsByVerificationCode(code));
        return code;
    }

    @Transactional
    public CredentialDtos.Response sign(UUID tenant, UUID id) {
        var c = findOwnedForUpdate(id, tenant);
        if (c.getStatus() != CredentialStatus.DRAFT)
            throw new IllegalArgumentException("Solo se firman borradores.");
        requests.approved(c);
        byte[] bytes = checkedDocument(c);
        safety.validate(bytes);
        var result = signing.sign(tenant, bytes);
        safety.validate(result.bytes());
        String key = storage.store(tenant, id, "CREDENTIAL", result.bytes(), "application/pdf");
        c.attachDocument(key, c.getDocumentName(), "application/pdf", sha256(result.bytes()));
        c.signed(
                result.fingerprint(),
                result.certificate(),
                java.time.Instant.now(),
                result.testOnly());
        audit.record(CREDENTIAL_SIGNED, tenant, CREDENTIAL, id);
        return CredentialDtos.Response.from(c);
    }

    @Transactional(readOnly = true)
    public CredentialDtos.StoredDocument personalDocument(UUID student, UUID id) {
        var c =
                credentialRepository
                        .findById(id)
                        .filter(x ->
                                x.getStudentAccount() != null
                                        && x.getStudentAccount().getId().equals(student)
                                        && x.getStatus() != CredentialStatus.DRAFT)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Documento no encontrado."));
        return new CredentialDtos.StoredDocument(
                checkedDocument(c), "application/pdf", c.getDocumentName());
    }

    public record DocumentCheck(
            boolean hashMatches,
            bo.edu.uagrm.credenciales.signing.SigningService.Check signature,
            String registryStatus,
            String issuerStatus,
            boolean testSignature) {}

    @Transactional(readOnly = true)
    public DocumentCheck checkDocument(String code, MultipartFile file) {
        var c =
                credentialRepository
                        .findByVerificationCode(code.strip().toUpperCase(java.util.Locale.ROOT))
                        .filter(x -> x.getStatus() != CredentialStatus.DRAFT)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Credencial no encontrada."));
        try {
            byte[] bytes = file.getBytes();
            safety.validate(bytes);
            return new DocumentCheck(
                    sha256(bytes).equals(c.getDocumentHash()),
                    signing.verify(bytes, c.getSignerFingerprint()),
                    c.getStatus().name(),
                    issuerStatus(c),
                    c.isSignatureTestOnly());
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el documento.");
        }
    }

    private byte[] checkedDocument(Credential c) {
        if (c.getDocumentObjectKey() == null)
            throw new IllegalArgumentException("Adjunta o genera el PDF primero.");
        byte[] bytes = storage.load(c.getDocumentObjectKey());
        if (bytes == null || !sha256(bytes).equals(c.getDocumentHash()))
            throw new IllegalStateException("La integridad del archivo almacenado no coincide.");
        return bytes;
    }

    private String issuerStatus(Credential c) {
        return governance
                .findById(c.getTenant().getId())
                .map(g -> g.getStatus().name())
                .orElse(c.getTenant().isActive() ? "ACTIVE" : "INACTIVE");
    }

    private CredentialDtos.PublicVerification publicView(Credential c) {
        return CredentialDtos.PublicVerification.from(c)
                .withDetails(
                        issuerStatus(c),
                        c.getReplacesId() == null
                                ? null
                                : credentialRepository
                                        .findById(c.getReplacesId())
                                        .map(Credential::getVerificationCode)
                                        .orElse(null),
                        c.getReplacedById() == null
                                ? null
                                : credentialRepository
                                        .findById(c.getReplacedById())
                                        .map(Credential::getVerificationCode)
                                        .orElse(null));
    }

    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }
}
