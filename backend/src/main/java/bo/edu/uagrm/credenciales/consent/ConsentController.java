package bo.edu.uagrm.credenciales.consent;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/me/consents") @PreAuthorize("hasRole('STUDENT')")
public class ConsentController {
    private final ConsentService service;
    public ConsentController(ConsentService service){this.service=service;}
    public record Request(@NotBlank @Size(max=80) String institutionSlug) {}
    @GetMapping
    List<ConsentService.Access> history(@AuthenticationPrincipal UserPrincipal p){return service.history(p.userId());}
    @PostMapping
    ConsentService.Generated generate(@AuthenticationPrincipal UserPrincipal p,@Valid @RequestBody Request r){
        return service.generate(p.userId(),r.institutionSlug());
    }
    @DeleteMapping("/{id}")
    void revoke(@AuthenticationPrincipal UserPrincipal p,@PathVariable UUID id){service.revoke(p.userId(),id);}
}
