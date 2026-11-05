package bo.edu.uagrm.credenciales.credential;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/me/credentials") @PreAuthorize("hasRole('STUDENT')")
public class StudentHistoryController {
    private final CredentialService service;
    public StudentHistoryController(CredentialService service){this.service=service;}
    @GetMapping
    List<CredentialDtos.PublicVerification> list(@AuthenticationPrincipal UserPrincipal p){
        return service.personalHistory(p.userId());
    }
}
