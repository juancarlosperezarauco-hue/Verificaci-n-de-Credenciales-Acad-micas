package bo.edu.uagrm.credenciales.student;

import bo.edu.uagrm.credenciales.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final bo.edu.uagrm.credenciales.consent.ConsentService consents;

    public StudentController(bo.edu.uagrm.credenciales.consent.ConsentService consents) {
        this.consents = consents;
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('STUDENT_VIEW')")
    List<bo.edu.uagrm.credenciales.identity.IdentityDtos.Profile> list(@AuthenticationPrincipal UserPrincipal principal) {
        return consents.linked(principal.tenantId());
    }

    public record Redeem(@jakarta.validation.constraints.NotBlank
                         @jakarta.validation.constraints.Pattern(regexp="(?i)(EST-)?[a-f0-9]{32}") String code) {}
    @PostMapping("/redeem")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('STUDENT_LINK')")
    bo.edu.uagrm.credenciales.identity.IdentityDtos.Profile redeem(@AuthenticationPrincipal UserPrincipal principal,
                                @Valid @RequestBody Redeem request) {
        return consents.redeem(principal.tenantId(), principal.userId(), request.code());
    }
}
