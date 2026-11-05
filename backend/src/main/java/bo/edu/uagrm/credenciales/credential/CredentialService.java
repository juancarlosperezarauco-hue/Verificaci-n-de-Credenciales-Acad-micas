package bo.edu.uagrm.credenciales.credential;

import bo.edu.uagrm.credenciales.blockchain.BlockchainAnchorPort;
import bo.edu.uagrm.credenciales.storage.DocumentStoragePort;
import bo.edu.uagrm.credenciales.student.Student;
import bo.edu.uagrm.credenciales.student.StudentRepository;
import bo.edu.uagrm.credenciales.tenant.Tenant;
import bo.edu.uagrm.credenciales.tenant.TenantRepository;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CredentialService {
    private final CredentialRepository credentialRepository;
    private final TenantRepository tenantRepository;
    private final bo.edu.uagrm.credenciales.consent.ConsentService consents;
    private final BlockchainAnchorPort blockchain;
    private final DocumentStoragePort storage;
    private final CredentialPdfService pdfService;

    public CredentialService(CredentialRepository credentialRepository, TenantRepository tenantRepository,
                             bo.edu.uagrm.credenciales.consent.ConsentService consents, BlockchainAnchorPort blockchain,
                             DocumentStoragePort storage, CredentialPdfService pdfService) {
        this.credentialRepository = credentialRepository;
        this.tenantRepository = tenantRepository;
        this.consents = consents;
        this.blockchain = blockchain;
        this.storage = storage;
        this.pdfService = pdfService;
    }

    @Transactional(readOnly = true)
    public List<CredentialDtos.Response> list(UUID tenantId) {
        return credentialRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(CredentialDtos.Response::from).toList();
    }

    @Transactional
    public CredentialDtos.Response create(UUID tenantId, CredentialDtos.CreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Organización no encontrada"));
        var student = consents.authorizedStudent(tenantId, request.studentId());
        Credential credential = new Credential(tenant, student, request.credentialType().trim(),
                request.title().trim());
        return CredentialDtos.Response.from(credentialRepository.save(credential));
    }

    @Transactional
    public List<CredentialDtos.Response> createBatch(UUID tenantId, CredentialDtos.CreateBatchRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Organización no encontrada"));
        return request.studentIds().stream().distinct().map(studentId -> {
            var student = consents.authorizedStudent(tenantId, studentId);
            Credential credential = new Credential(tenant, student, request.credentialType().trim(),
                    request.title().trim());
            return CredentialDtos.Response.from(credentialRepository.save(credential));
        }).toList();
    }

    @Transactional
    public CredentialDtos.Response attachDocument(UUID tenantId, UUID id, MultipartFile file) {
        Credential credential = findOwned(id, tenantId);
        if (file.isEmpty()) throw new IllegalArgumentException("El documento está vacío");
        try {
            byte[] content = file.getBytes();
            String originalName = file.getOriginalFilename() == null ? "documento.pdf" : file.getOriginalFilename();
            String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String objectKey = tenantId + "/" + id + "/" + safeName;
            String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
            String storedKey = storage.store(objectKey, content, contentType);
            credential.attachDocument(storedKey, originalName, contentType, sha256(content));
            return CredentialDtos.Response.from(credential);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el documento", exception);
        }
    }

    @Transactional
    public CredentialDtos.Response issue(UUID tenantId, UUID id) {
        Credential credential = findOwned(id, tenantId);
        if (credential.getStudentAccount() != null) {
            consents.authorizedStudent(tenantId, credential.getStudentAccount().getId());
        }
        String code = credential.getVerificationCode() == null ? uniqueVerificationCode()
                : credential.getVerificationCode();
        if (credential.getDocumentHash() == null) {
            throw new IllegalStateException("Debe adjuntar el documento antes de emitir la credencial");
        }
        String transactionId = blockchain.anchor(tenantId, id, credential.getDocumentHash());
        credential.issue(code, transactionId);
        return CredentialDtos.Response.from(credential);
    }

    @Transactional
    public CredentialDtos.Response generateDocument(UUID tenantId, UUID id) {
        Credential credential = findOwned(id, tenantId);
        if (credential.getStatus() != CredentialStatus.DRAFT) {
            throw new IllegalStateException("Solo se puede generar el documento de un borrador");
        }
        credential.prepareVerificationCode(uniqueVerificationCode());
        byte[] content = pdfService.generate(credential);
        String fileName = credential.getCredentialType().toLowerCase() + "-"
                + credential.getHolderName().replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ]+", "-") + ".pdf";
        String objectKey = tenantId + "/" + id + "/generated/" + fileName;
        storage.store(objectKey, content, "application/pdf");
        credential.attachDocument(objectKey, fileName, "application/pdf", sha256(content));
        return CredentialDtos.Response.from(credential);
    }

    @Transactional(readOnly = true)
    public CredentialDtos.StoredDocument document(UUID tenantId, UUID id) {
        Credential credential = findOwned(id, tenantId);
        if (credential.getDocumentObjectKey() == null) {
            throw new IllegalArgumentException("La credencial no tiene un documento adjunto");
        }
        return new CredentialDtos.StoredDocument(storage.load(credential.getDocumentObjectKey()),
                credential.getDocumentContentType(), credential.getDocumentName());
    }

    @Transactional
    public CredentialDtos.Response revoke(UUID tenantId, UUID id, String reason) {
        Credential credential = findOwned(id, tenantId);
        credential.revoke(reason.trim());
        return CredentialDtos.Response.from(credential);
    }

    @Transactional(readOnly = true)
    public CredentialDtos.PublicVerification verify(String code) {
        return credentialRepository.findByVerificationCode(code.trim().toUpperCase())
                .filter(credential -> credential.getStatus() != CredentialStatus.DRAFT)
                .map(CredentialDtos.PublicVerification::from)
                .orElseThrow(() -> new IllegalArgumentException("Credencial no encontrada"));
    }

    private Credential findOwned(UUID id, UUID tenantId) {
        return credentialRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Credencial no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<CredentialDtos.PublicVerification> personalHistory(UUID studentId) {
        return credentialRepository.findAllByStudentAccountIdOrderByCreatedAtDesc(studentId).stream()
            .filter(c -> c.getStatus() != CredentialStatus.DRAFT)
            .map(CredentialDtos.PublicVerification::from).toList();
    }

    private String uniqueVerificationCode() {
        String code;
        do {
            code = "UAGRM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (credentialRepository.existsByVerificationCode(code));
        return code;
    }

    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }
}

