package bo.edu.uagrm.credenciales.tenant;
import bo.edu.uagrm.credenciales.auth.AuthDtos;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController
public class OrganizationOnboardingController {
 private final OrganizationOnboardingService service;
 public OrganizationOnboardingController(OrganizationOnboardingService service){this.service=service;}
 @PostMapping("/api/auth/organizations/register") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
 public AuthDtos.AuthResponse register(@Valid @RequestBody OrganizationDtos.Registration r){return service.register(r);}
 @GetMapping("/api/institution/details") @PreAuthorize("hasAuthority('ORGANIZATION_VIEW')")
 public OrganizationDtos.Details get(@AuthenticationPrincipal UserPrincipal p){return service.get(p.tenantId());}
 @PutMapping("/api/institution/details") @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE')")
 public OrganizationDtos.Details update(@AuthenticationPrincipal UserPrincipal p,@Valid @RequestBody OrganizationDtos.Details d){return service.update(p.tenantId(),d);}
 @GetMapping("/api/platform/organizations/{id}/details") @PreAuthorize("hasRole('SUPER_ADMIN')")
 public OrganizationDtos.Details platform(@PathVariable UUID id){return service.get(id);}
}
