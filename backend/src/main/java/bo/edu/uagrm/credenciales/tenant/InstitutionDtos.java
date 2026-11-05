package bo.edu.uagrm.credenciales.tenant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class InstitutionDtos {
    private InstitutionDtos() {}

    public record UpdateBranding(
            @NotNull @Pattern(regexp = "#[0-9A-Fa-f]{6}") String primaryColor,
            @NotNull @Pattern(regexp = "#[0-9A-Fa-f]{6}") String secondaryColor,
            @NotNull @Pattern(regexp = "#[0-9A-Fa-f]{6}") String accentColor,
            @NotNull BrandingStyle brandingStyle,
            @Size(max = 180) String signatoryName,
            @Size(max = 180) String signatoryTitle) {}

    public record Profile(
            UUID id,
            String name,
            String slug,
            InstitutionType institutionType,
            String primaryColor,
            String secondaryColor,
            String accentColor,
            BrandingStyle brandingStyle,
            String logoName,
            String logoUrl,
            String verificationDocumentName,
            String verificationDocumentHash,
            String signatoryName,
            String signatoryTitle) {
        static Profile from(Tenant tenant) {
            String logoUrl = tenant.getLogoObjectKey() == null ? null
                    : "/api/public/institutions/" + tenant.getId() + "/logo?v="
                    + Integer.toUnsignedString(tenant.getLogoObjectKey().hashCode());
            return new Profile(tenant.getId(), tenant.getName(), tenant.getSlug(), tenant.getInstitutionType(),
                    tenant.getPrimaryColor(), tenant.getSecondaryColor(), tenant.getAccentColor(),
                    tenant.getBrandingStyle(), tenant.getLogoName(), logoUrl,
                    tenant.getVerificationDocumentName(), tenant.getVerificationDocumentHash(),
                    tenant.getSignatoryName(), tenant.getSignatoryTitle());
        }
    }

    public record StoredFile(byte[] content, String contentType, String name) {}
}
