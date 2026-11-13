package bo.edu.uagrm.credenciales.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class Tenant {
    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "institution_type", nullable = false, length = 40)
    private InstitutionType institutionType;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "primary_color", nullable = false, length = 7)
    private String primaryColor;

    @Column(name = "secondary_color", nullable = false, length = 7)
    private String secondaryColor;

    @Column(name = "accent_color", nullable = false, length = 7)
    private String accentColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "branding_style", nullable = false, length = 30)
    private BrandingStyle brandingStyle;

    @Column(name = "logo_object_key", length = 500)
    private String logoObjectKey;

    @Column(name = "logo_name", length = 255)
    private String logoName;

    @Column(name = "logo_content_type", length = 120)
    private String logoContentType;

    @Column(name = "verification_document_object_key", length = 500)
    private String verificationDocumentObjectKey;

    @Column(name = "verification_document_name", length = 255)
    private String verificationDocumentName;

    @Column(name = "verification_document_content_type", length = 120)
    private String verificationDocumentContentType;

    @Column(name = "verification_document_hash", length = 64)
    private String verificationDocumentHash;

    @Column(name = "signatory_name", length = 180)
    private String signatoryName;

    @Column(name = "signatory_title", length = 180)
    private String signatoryTitle;

    protected Tenant() {}

    public Tenant(String name, String slug, InstitutionType institutionType) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.slug = slug;
        this.institutionType = institutionType;
        this.active = true;
        this.createdAt = Instant.now();
        this.primaryColor = "#22D3EE";
        this.secondaryColor = "#2563EB";
        this.accentColor = "#A855F7";
        this.brandingStyle = BrandingStyle.MODERNO;
    }

    public void updateBranding(String primaryColor, String secondaryColor, String accentColor,
                               BrandingStyle brandingStyle, String signatoryName, String signatoryTitle) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.accentColor = accentColor;
        this.brandingStyle = brandingStyle;
        this.signatoryName = blankToNull(signatoryName);
        this.signatoryTitle = blankToNull(signatoryTitle);
    }

    public void attachLogo(String objectKey, String name, String contentType) {
        this.logoObjectKey = objectKey;
        this.logoName = name;
        this.logoContentType = contentType;
    }

    public void attachVerificationDocument(String objectKey, String name, String contentType, String hash) {
        this.verificationDocumentObjectKey = objectKey;
        this.verificationDocumentName = name;
        this.verificationDocumentContentType = contentType;
        this.verificationDocumentHash = hash;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public InstitutionType getInstitutionType() { return institutionType; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public void updateIdentification(String name, InstitutionType institutionType) {
        this.name = name;
        this.institutionType = institutionType;
    }
    public String getPrimaryColor() { return primaryColor; }
    public String getSecondaryColor() { return secondaryColor; }
    public String getAccentColor() { return accentColor; }
    public BrandingStyle getBrandingStyle() { return brandingStyle; }
    public String getLogoObjectKey() { return logoObjectKey; }
    public String getLogoName() { return logoName; }
    public String getLogoContentType() { return logoContentType; }
    public String getVerificationDocumentObjectKey() { return verificationDocumentObjectKey; }
    public String getVerificationDocumentName() { return verificationDocumentName; }
    public String getVerificationDocumentContentType() { return verificationDocumentContentType; }
    public String getVerificationDocumentHash() { return verificationDocumentHash; }
    public String getSignatoryName() { return signatoryName; }
    public String getSignatoryTitle() { return signatoryTitle; }
}

