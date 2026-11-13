package bo.edu.uagrm.credenciales.identity;

import bo.edu.uagrm.credenciales.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/security")
@PreAuthorize("hasRole('STUDENT')")
public class StudentSecurityController {
    private final StudentSecurityService service;
    public StudentSecurityController(StudentSecurityService service) { this.service=service; }
    @GetMapping
    StudentSecurityDtos.Snapshot snapshot(@AuthenticationPrincipal UserPrincipal principal) { return service.snapshot(principal.userId()); }
    @PostMapping("/email-verification") @ResponseStatus(HttpStatus.NO_CONTENT)
    void resend(@AuthenticationPrincipal UserPrincipal principal) { service.resendVerification(principal.userId()); }
    @PostMapping("/password") @ResponseStatus(HttpStatus.NO_CONTENT)
    void password(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody StudentSecurityDtos.PasswordChange request) {
        service.changePassword(principal.userId(), request.currentPassword(), request.newPassword());
    }
    @PostMapping("/email-change") @ResponseStatus(HttpStatus.NO_CONTENT)
    void email(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody StudentSecurityDtos.EmailChange request) {
        service.requestEmailChange(principal.userId(), request.currentPassword(), request.newEmail());
    }
    @DeleteMapping("/account") @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody StudentSecurityDtos.Deactivate request) {
        service.deactivate(principal.userId(), request.currentPassword());
    }
}
