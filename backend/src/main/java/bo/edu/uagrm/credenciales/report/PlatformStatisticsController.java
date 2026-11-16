package bo.edu.uagrm.credenciales.report;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class PlatformStatisticsController {
    private final ReportService service;

    public PlatformStatisticsController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/api/platform/statistics")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ReportService.Platform statistics() {
        return service.platform();
    }
}
