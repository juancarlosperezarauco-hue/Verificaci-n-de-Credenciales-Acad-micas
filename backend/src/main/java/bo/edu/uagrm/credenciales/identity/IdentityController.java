package bo.edu.uagrm.credenciales.identity;
import bo.edu.uagrm.credenciales.auth.AuthDtos;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class IdentityController {
    private final IdentityService identities;
    public IdentityController(IdentityService identities) { this.identities=identities; }
    @PostMapping("/api/auth/student/register") @ResponseStatus(HttpStatus.CREATED)
    AuthDtos.AuthResponse register(@Valid @RequestBody IdentityDtos.Register request) { return identities.register(request); }
    @PostMapping("/api/auth/student/login")
    AuthDtos.AuthResponse login(@Valid @RequestBody IdentityDtos.Login request) { return identities.login(request); }
    @GetMapping("/api/me/profile") @PreAuthorize("hasRole('STUDENT')")
    IdentityDtos.OwnProfile profile(@AuthenticationPrincipal UserPrincipal p) { return identities.profile(p.userId()); }
    @PostMapping("/api/me/search-code/rotate") @PreAuthorize("hasRole('STUDENT')")
    IdentityDtos.OwnProfile rotate(@AuthenticationPrincipal UserPrincipal p) {
        return identities.rotateSearchCode(p.userId());
    }
}
