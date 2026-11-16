package bo.edu.uagrm.credenciales.credential;

import bo.edu.uagrm.credenciales.platform.GovernanceRepository;
import bo.edu.uagrm.credenciales.signing.SigningService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Read-only views keep public verification separate from institutional mutations. */
@Service
@Transactional(readOnly = true)
public class CredentialVerificationService {
    private final CredentialRepository credentials;
    private final GovernanceRepository governance;
    private final CredentialDocumentService documents;
    private final SigningService signing;

    public CredentialVerificationService(
            CredentialRepository credentials,
            GovernanceRepository governance,
            CredentialDocumentService documents,
            SigningService signing) {
        this.credentials = credentials;
        this.governance = governance;
        this.documents = documents;
        this.signing = signing;
    }

    public CredentialDtos.PublicVerification verify(String code) {
        return view(published(code));
    }

    public List<CredentialDtos.PublicVerification> personalHistory(UUID studentId) {
        return credentials.findAllByStudentAccountIdOrderByCreatedAtDesc(studentId).stream()
                .filter(credential -> credential.getStatus() != CredentialStatus.DRAFT)
                .map(this::view)
                .toList();
    }

    public CredentialDtos.StoredDocument personalDocument(UUID studentId, UUID credentialId) {
        Credential credential =
                credentials
                        .findById(credentialId)
                        .filter(row ->
                                row.getStudentAccount() != null
                                        && row.getStudentAccount().getId().equals(studentId)
                                        && row.getStatus() != CredentialStatus.DRAFT)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Documento no encontrado."));
        return new CredentialDtos.StoredDocument(
                documents.read(credential), "application/pdf", credential.getDocumentName());
    }

    public CredentialDtos.DocumentCheck checkDocument(String code, MultipartFile file) {
        Credential credential = published(code);
        try {
            byte[] content = file.getBytes();
            boolean matches = documents.matches(credential, content);
            return new CredentialDtos.DocumentCheck(
                    matches,
                    signing.verify(content, credential.getSignerFingerprint()),
                    credential.getStatus().name(),
                    issuerStatus(credential),
                    credential.isSignatureTestOnly());
        } catch (IOException exception) {
            throw new IllegalArgumentException("No se pudo leer el documento.");
        }
    }

    private Credential published(String code) {
        return credentials
                .findByVerificationCode(code.strip().toUpperCase(Locale.ROOT))
                .filter(credential -> credential.getStatus() != CredentialStatus.DRAFT)
                .orElseThrow(() -> new IllegalArgumentException("Credencial no encontrada"));
    }

    private String issuerStatus(Credential credential) {
        return governance
                .findById(credential.getTenant().getId())
                .map(row -> row.getStatus().name())
                .orElse(credential.getTenant().isActive() ? "ACTIVE" : "INACTIVE");
    }

    CredentialDtos.PublicVerification view(Credential credential) {
        return CredentialDtos.PublicVerification.from(credential)
                .withDetails(
                        issuerStatus(credential),
                        verificationCode(credential.getReplacesId()),
                        verificationCode(credential.getReplacedById()));
    }

    private String verificationCode(UUID id) {
        return id == null
                ? null
                : credentials.findById(id).map(Credential::getVerificationCode).orElse(null);
    }
}
