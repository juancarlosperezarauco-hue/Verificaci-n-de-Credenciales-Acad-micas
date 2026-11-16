package bo.edu.uagrm.credenciales.template;

import bo.edu.uagrm.credenciales.credential.*;
import bo.edu.uagrm.credenciales.identity.StudentAccount;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.shared.*;

import jakarta.validation.Valid;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/institution/templates")
public class TemplateController {
    private final TemplateService service;
    private final TenantAccess access;
    private final CredentialPdfService pdf;

    public TemplateController(TemplateService s, TenantAccess a, CredentialPdfService p) {
        service = s;
        access = a;
        pdf = p;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TEMPLATE_VIEW')")
    List<TemplateService.View> list(@AuthenticationPrincipal UserPrincipal p) {
        return service.list(p);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TEMPLATE_MANAGE')")
    TemplateService.View create(
            @AuthenticationPrincipal UserPrincipal p,
            @Valid @RequestBody TemplateService.Input input) {
        return service.create(p, input);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAuthority('TEMPLATE_MANAGE')")
    TemplateService.View active(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> input) {
        return service.activate(p, id, Boolean.TRUE.equals(input.get("active")));
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAuthority('TEMPLATE_VIEW')")
    ResponseEntity<byte[]> preview(
            @AuthenticationPrincipal UserPrincipal p, @PathVariable UUID id) {
        UUID tenant = access.require(p, "TEMPLATE_VIEW");
        var template = service.require(tenant, id, null, false);
        var c =
                new Credential(
                        access.active(tenant),
                        new StudentAccount(
                                "Estudiante de muestra",
                                "BO",
                                "CI",
                                "MUESTRA",
                                LocalDate.of(2000, 1, 1),
                                "Muestra",
                                "muestra@example.invalid",
                                "unused"),
                        template.getCredentialType(),
                        "Programa de muestra");
        c.prepareVerificationCode("MUESTRA-SIN-VALIDEZ");
        return PrivateDownloads.of(
                new CredentialDtos.StoredDocument(
                        pdf.generate(c, template, "Programa de muestra", true),
                        "application/pdf",
                        "muestra-sin-validez.pdf"));
    }
}
