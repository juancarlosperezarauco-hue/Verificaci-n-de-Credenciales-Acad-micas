package bo.edu.uagrm.credenciales.credential;

import bo.edu.uagrm.credenciales.storage.ManagedStorage;

import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Owns validation, storage and integrity of credential PDF bytes. */
@Service
public class CredentialDocumentService {
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private final ManagedStorage storage;
    private final PdfSafety safety;

    public CredentialDocumentService(ManagedStorage storage, PdfSafety safety) {
        this.storage = storage;
        this.safety = safety;
    }

    /** Caller owns the credential lock and business transaction. Never overwrites an object. */
    public void attach(Credential credential, byte[] content, String name) {
        safety.validate(content);
        String key =
                storage.store(
                        credential.getTenant().getId(),
                        credential.getId(),
                        "CREDENTIAL",
                        content,
                        PDF_CONTENT_TYPE);
        credential.attachDocument(key, name, PDF_CONTENT_TYPE, hash(content));
    }

    public byte[] read(Credential credential) {
        if (credential.getDocumentObjectKey() == null) {
            throw new IllegalArgumentException("Adjunta o genera el PDF primero.");
        }
        byte[] content = storage.load(credential.getDocumentObjectKey());
        if (content == null || !hash(content).equals(credential.getDocumentHash())) {
            throw new IllegalStateException("La integridad del archivo almacenado no coincide.");
        }
        return content;
    }

    public byte[] readValidated(Credential credential) {
        byte[] content = read(credential);
        safety.validate(content);
        return content;
    }

    public boolean matches(Credential credential, byte[] content) {
        safety.validate(content);
        return hash(content).equals(credential.getDocumentHash());
    }

    private String hash(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }
}
