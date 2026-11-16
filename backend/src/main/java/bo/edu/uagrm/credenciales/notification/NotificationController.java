package bo.edu.uagrm.credenciales.notification;

import bo.edu.uagrm.credenciales.security.UserPrincipal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/me/notifications")
@PreAuthorize("hasRole('STUDENT')")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public NotificationService.Inbox list(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.inbox(p.userId(), unreadOnly, page, size);
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void read(@AuthenticationPrincipal UserPrincipal p, @PathVariable UUID id) {
        service.read(p.userId(), id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void readAll(@AuthenticationPrincipal UserPrincipal p) {
        service.readAll(p.userId());
    }
}
