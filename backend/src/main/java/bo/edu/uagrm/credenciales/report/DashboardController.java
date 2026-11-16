package bo.edu.uagrm.credenciales.report;

import bo.edu.uagrm.credenciales.security.UserPrincipal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/api/institution/dashboard")
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW')")
    public DashboardService.Snapshot snapshot(@AuthenticationPrincipal UserPrincipal p) {
        return service.snapshot(p);
    }
}
