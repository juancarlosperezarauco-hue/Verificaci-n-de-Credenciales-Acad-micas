package bo.edu.uagrm.credenciales.tenant;

import bo.edu.uagrm.credenciales.storage.DocumentStoragePort;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class InstitutionService {
    private static final Set<String> LOGO_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    private static final Set<String> VERIFICATION_TYPES = Set.of("application/pdf", "image/png", "image/jpeg");

    private final TenantRepository tenants;
    private final DocumentStoragePort storage;

    public InstitutionService(TenantRepository tenants, DocumentStoragePort storage) {
        this.tenants = tenants;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public InstitutionDtos.Profile profile(UUID tenantId) {
        return InstitutionDtos.Profile.from(find(tenantId));
    }

    @Transactional
    public InstitutionDtos.Profile update(UUID tenantId, InstitutionDtos.UpdateBranding request) {
        Tenant tenant = find(tenantId);
        tenant.updateBranding(request.primaryColor().toUpperCase(), request.secondaryColor().toUpperCase(),
                request.accentColor().toUpperCase(), request.brandingStyle(), request.signatoryName(),
                request.signatoryTitle());
        return InstitutionDtos.Profile.from(tenant);
    }

    @Transactional
    public InstitutionDtos.Profile uploadLogo(UUID tenantId, MultipartFile file) {
        validate(file, LOGO_TYPES, 2 * 1024 * 1024, "El logo debe ser PNG, JPG o WebP y pesar hasta 2 MB");
        Tenant tenant = find(tenantId);
        String name = originalName(file, "logo.png");
        String key = tenantId + "/institution/logo/" + UUID.randomUUID() + extension(name);
        storage.store(key, bytes(file), file.getContentType());
        tenant.attachLogo(key, name, file.getContentType());
        return InstitutionDtos.Profile.from(tenant);
    }

    @Transactional
    public InstitutionDtos.Profile uploadVerificationDocument(UUID tenantId, MultipartFile file) {
        validate(file, VERIFICATION_TYPES, 10 * 1024 * 1024,
                "El respaldo institucional debe ser PDF, PNG o JPG y pesar hasta 10 MB");
        Tenant tenant = find(tenantId);
        byte[] content = bytes(file);
        String name = originalName(file, "respaldo-institucional.pdf");
        String key = tenantId + "/institution/verification/" + UUID.randomUUID() + extension(name);
        storage.store(key, content, file.getContentType());
        tenant.attachVerificationDocument(key, name, file.getContentType(), sha256(content));
        return InstitutionDtos.Profile.from(tenant);
    }

    @Transactional(readOnly = true)
    public InstitutionDtos.StoredFile logo(UUID tenantId) {
        Tenant tenant = find(tenantId);
        if (tenant.getLogoObjectKey() == null) throw new IllegalArgumentException("La institución no tiene logo");
        return new InstitutionDtos.StoredFile(storage.load(tenant.getLogoObjectKey()),
                tenant.getLogoContentType(), tenant.getLogoName());
    }

    @Transactional(readOnly = true)
    public InstitutionDtos.StoredFile verificationDocument(UUID tenantId) {
        Tenant tenant = find(tenantId);
        if (tenant.getVerificationDocumentObjectKey() == null) {
            throw new IllegalArgumentException("La institución no tiene documento de respaldo");
        }
        return new InstitutionDtos.StoredFile(storage.load(tenant.getVerificationDocumentObjectKey()),
                tenant.getVerificationDocumentContentType(), tenant.getVerificationDocumentName());
    }

    private Tenant find(UUID tenantId) {
        return tenants.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));
    }

    private void validate(MultipartFile file, Set<String> allowedTypes, long maxSize, String message) {
        if (file.isEmpty() || file.getContentType() == null || !allowedTypes.contains(file.getContentType())
                || file.getSize() > maxSize) {
            throw new IllegalArgumentException(message);
        }
    }

    private byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el archivo", exception);
        }
    }

    private String originalName(MultipartFile file, String fallback) {
        String value = file.getOriginalFilename();
        return value == null || value.isBlank() ? fallback : value.replaceAll("[\\r\\n]", "");
    }

    private String extension(String name) {
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index).replaceAll("[^a-zA-Z0-9.]", "").toLowerCase();
    }

    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }
}
