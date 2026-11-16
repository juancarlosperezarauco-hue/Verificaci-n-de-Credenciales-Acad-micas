package bo.edu.uagrm.credenciales.credential;

import bo.edu.uagrm.credenciales.security.UserPrincipal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/me/credentials")
@PreAuthorize("hasRole('STUDENT')")
public class StudentHistoryController {
    private final CredentialService service;
    private final StudentHistoryService history;

    public StudentHistoryController(CredentialService service, StudentHistoryService history) {
        this.history = history;
        this.service = service;
    }

    @GetMapping
    List<CredentialDtos.PublicVerification> list(@AuthenticationPrincipal UserPrincipal p) {
        return service.personalHistory(p.userId());
    }

    @GetMapping("/page")
    public bo.edu.uagrm.credenciales.shared.ResultPage<CredentialDtos.PublicVerification> page(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(required = false) java.util.UUID institutionId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return history.page(p.userId(), institutionId, year, type, page, size);
    }

    @GetMapping("/filters")
    public StudentHistoryService.Filters filters(@AuthenticationPrincipal UserPrincipal p) {
        return history.filters(p.userId());
    }

    @GetMapping("/{id}/document")
    org.springframework.http.ResponseEntity<byte[]> document(
            @AuthenticationPrincipal UserPrincipal p, @PathVariable java.util.UUID id) {
        return bo.edu.uagrm.credenciales.shared.PrivateDownloads.of(
                service.personalDocument(p.userId(), id));
    }
}
