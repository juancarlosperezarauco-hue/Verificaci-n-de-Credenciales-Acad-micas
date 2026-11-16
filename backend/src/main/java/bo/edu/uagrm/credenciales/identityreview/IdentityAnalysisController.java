package bo.edu.uagrm.credenciales.identityreview;

import bo.edu.uagrm.credenciales.security.UserPrincipal;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@PreAuthorize("hasAuthority('IDENTITY_REVIEW')")
public class IdentityAnalysisController {
    private final IdentityAnalysisService service;

    public IdentityAnalysisController(IdentityAnalysisService service) {
        this.service = service;
    }

    @GetMapping("/api/institution/identity-analysis/status")
    public IdentityAnalysisService.Availability status() {
        return service.availability();
    }

    @PostMapping("/api/institution/identity-verifications/{id}/analysis")
    public ResponseEntity<IdentityAnalysisService.Result> analyze(
            @AuthenticationPrincipal UserPrincipal p, @PathVariable UUID id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer")
                .body(service.analyze(p, id));
    }
}
