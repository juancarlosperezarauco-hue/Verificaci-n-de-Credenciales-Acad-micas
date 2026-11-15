package bo.edu.uagrm.credenciales.signing;

import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.shared.TenantAccess;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/institution/signing-key")
public class SigningController {
    private final SigningService service;

    public SigningController(SigningService s) {
        service = s;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SIGNING_MANAGE','CREDENTIAL_SIGN','CREDENTIAL_VIEW')")
    SigningService.Status status(@AuthenticationPrincipal UserPrincipal p) {
        if (p.tenantId() == null) throw TenantAccess.missing();
        return service.status(p.tenantId());
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('SIGNING_MANAGE')")
    SigningService.Status configure(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestPart MultipartFile file,
            @RequestParam String password,
            @RequestParam(defaultValue = "false") boolean required) {
        return service.configure(p, file, password, required);
    }

    @PostMapping("/demo")
    @PreAuthorize("hasAuthority('SIGNING_MANAGE')")
    SigningService.Status demo(
            @AuthenticationPrincipal UserPrincipal p, @RequestBody Map<String, Boolean> input) {
        return service.createDemo(p, Boolean.TRUE.equals(input.get("required")));
    }

    @PatchMapping("/policy")
    @PreAuthorize("hasAuthority('SIGNING_MANAGE')")
    SigningService.Status policy(
            @AuthenticationPrincipal UserPrincipal p, @RequestBody Map<String, Boolean> input) {
        return service.policy(p, Boolean.TRUE.equals(input.get("required")));
    }
}
