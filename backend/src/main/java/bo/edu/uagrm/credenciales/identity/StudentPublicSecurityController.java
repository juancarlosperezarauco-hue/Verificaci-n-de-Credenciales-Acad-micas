package bo.edu.uagrm.credenciales.identity;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/student")
public class StudentPublicSecurityController {
    private final StudentSecurityService service;
    public StudentPublicSecurityController(StudentSecurityService service) { this.service=service; }
    @PostMapping("/verify-email") @ResponseStatus(HttpStatus.NO_CONTENT)
    void verify(@Valid @RequestBody StudentSecurityDtos.TokenRequest request) { service.verifyEmail(request.token()); }
    @PostMapping("/password-reset/request") @ResponseStatus(HttpStatus.NO_CONTENT)
    void requestReset(@Valid @RequestBody StudentSecurityDtos.PasswordResetRequest request) { service.requestPasswordReset(request.email()); }
    @PostMapping("/password-reset/confirm") @ResponseStatus(HttpStatus.NO_CONTENT)
    void reset(@Valid @RequestBody StudentSecurityDtos.PasswordResetConfirm request) { service.resetPassword(request.token(), request.newPassword()); }
    @PostMapping("/email-change/confirm") @ResponseStatus(HttpStatus.NO_CONTENT)
    void confirmEmail(@Valid @RequestBody StudentSecurityDtos.TokenRequest request) { service.confirmEmailChange(request.token()); }
}
