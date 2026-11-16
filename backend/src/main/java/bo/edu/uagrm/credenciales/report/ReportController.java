package bo.edu.uagrm.credenciales.report;

import bo.edu.uagrm.credenciales.credential.*;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.shared.*;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/institution/reports")
@PreAuthorize("hasAuthority('REPORT_VIEW')")
public class ReportController {
    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ReportService.Summary summary(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) CredentialStatus status) {
        return service.summary(p, new CredentialFilters(year, type, status));
    }

    @GetMapping("/credentials")
    public ResultPage<ReportService.Row> page(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) CredentialStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.page(p, new CredentialFilters(year, type, status), page, size);
    }

    @GetMapping("/credentials.{format:pdf|xlsx}")
    public ResponseEntity<byte[]> export(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable String format,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) CredentialStatus status) {
        return PrivateDownloads.of(
                service.export(
                        p, new CredentialFilters(year, type, status), format.equals("xlsx")));
    }
}
