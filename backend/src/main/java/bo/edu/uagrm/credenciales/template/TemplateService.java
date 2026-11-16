package bo.edu.uagrm.credenciales.template;

import bo.edu.uagrm.credenciales.audit.*;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.shared.TenantAccess;
import bo.edu.uagrm.credenciales.tenant.TenantRepository;

import jakarta.validation.constraints.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;

@Service
@Transactional
public class TemplateService {
    public record Input(
            UUID previousVersionId,
            @NotBlank @Size(max = 140) String name,
            @NotBlank @Pattern(regexp = "DIPLOMA|CERTIFICADO|TITULO|RECONOCIMIENTO|CONSTANCIA")
                    String credentialType,
            @NotBlank @Size(max = 180) String heading,
            @NotBlank @Size(max = 3000) String bodyText,
            @NotBlank @Pattern(regexp = "LANDSCAPE|PORTRAIT") String orientation) {}

    public record View(
            UUID id,
            UUID familyId,
            int version,
            String name,
            String credentialType,
            String heading,
            String bodyText,
            String orientation,
            boolean active) {}

    private final TemplateRepository templates;
    private final TenantAccess access;
    private final TenantRepository tenants;
    private final AuditTrail audit;
    private final Clock clock;

    public TemplateService(
            TemplateRepository t,
            TenantAccess a,
            TenantRepository tenants,
            AuditTrail audit,
            Clock c) {
        templates = t;
        access = a;
        this.tenants = tenants;
        this.audit = audit;
        clock = c;
    }

    @Transactional(readOnly = true)
    public List<View> list(UserPrincipal p) {
        return templates
                .findAllByTenantIdOrderByCreatedAtDesc(access.require(p, "TEMPLATE_VIEW"))
                .stream()
                .map(this::view)
                .toList();
    }

    public View create(UserPrincipal p, Input input) {
        UUID tenant = access.require(p, "TEMPLATE_MANAGE");
        validateText(input.heading());
        validateText(input.bodyText());
        tenants.findLockedById(tenant).orElseThrow(TenantAccess::missing);
        var row = new DocumentTemplate();
        row.id = UUID.randomUUID();
        row.tenantId = tenant;
        row.familyId = UUID.randomUUID();
        row.versionNumber = 1;
        if (input.previousVersionId() != null) {
            var previous = require(tenant, input.previousVersionId(), null, false);
            row.familyId = previous.familyId;
            row.versionNumber =
                    templates
                                    .findAllByFamilyIdOrderByVersionNumberDesc(row.familyId)
                                    .getFirst()
                                    .versionNumber
                            + 1;
        }
        row.name = input.name().strip();
        row.credentialType = input.credentialType();
        row.heading = input.heading().strip();
        row.bodyText = input.bodyText().strip();
        row.orientation = input.orientation();
        row.active = true;
        row.createdAt = clock.instant();
        templates.saveAndFlush(row);
        audit.record(AuditAction.TEMPLATE_CREATED, tenant, AuditTrail.Resource.TEMPLATE, row.id);
        return view(row);
    }

    public View activate(UserPrincipal p, UUID id, boolean active) {
        var row = require(access.require(p, "TEMPLATE_MANAGE"), id, null, false);
        row.active = active;
        return view(row);
    }

    public DocumentTemplate require(UUID tenant, UUID id, String type, boolean active) {
        var row = templates.findByIdAndTenantId(id, tenant).orElseThrow(TenantAccess::missing);
        if (active && !row.active)
            throw new IllegalArgumentException("La versión de plantilla está deshabilitada.");
        if (type != null && !type.equals(row.credentialType))
            throw new IllegalArgumentException("La plantilla no corresponde al tipo de documento.");
        return row;
    }

    public static void validateText(String text) {
        var matcher = java.util.regex.Pattern.compile("\\{\\{([^{}]+)}}").matcher(text);
        Set<String> variables =
                Set.of(
                        "studentName",
                        "institutionName",
                        "programName",
                        "documentTitle",
                        "issueDate",
                        "verificationCode");
        while (matcher.find())
            if (!variables.contains(matcher.group(1)))
                throw new IllegalArgumentException(
                        "Variable de plantilla no permitida: " + matcher.group(1));
        String rest = text.replaceAll("\\{\\{[^{}]+}}", "");
        if (rest.contains("{{") || rest.contains("}}") || text.indexOf('\0') >= 0)
            throw new IllegalArgumentException("Variable de plantilla incompleta.");
    }

    public static String render(String text, Map<String, String> values) {
        for (var value : values.entrySet())
            text =
                    text.replace(
                            "{{" + value.getKey() + "}}",
                            value.getValue() == null ? "" : value.getValue());
        return text;
    }

    public View view(DocumentTemplate t) {
        return new View(
                t.id,
                t.familyId,
                t.versionNumber,
                t.name,
                t.credentialType,
                t.heading,
                t.bodyText,
                t.orientation,
                t.active);
    }
}
