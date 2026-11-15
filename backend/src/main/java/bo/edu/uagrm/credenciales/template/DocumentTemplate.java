package bo.edu.uagrm.credenciales.template;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_templates")
public class DocumentTemplate {
    @Id UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "family_id", nullable = false)
    UUID familyId;

    @Column(name = "version_number", nullable = false)
    int versionNumber;

    @Column(nullable = false, length = 140)
    String name;

    @Column(name = "credential_type", nullable = false, length = 60)
    String credentialType;

    @Column(nullable = false, length = 180)
    String heading;

    @Column(name = "body_text", nullable = false, length = 3000)
    String bodyText;

    @Column(nullable = false, length = 20)
    String orientation;

    @Column(nullable = false)
    boolean active;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected DocumentTemplate() {}

    public UUID getId() {
        return id;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public String getHeading() {
        return heading;
    }

    public String getBodyText() {
        return bodyText;
    }

    public String getOrientation() {
        return orientation;
    }

    public boolean isActive() {
        return active;
    }
}
