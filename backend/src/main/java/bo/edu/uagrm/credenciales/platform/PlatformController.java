package bo.edu.uagrm.credenciales.platform;
import bo.edu.uagrm.credenciales.security.*;
import bo.edu.uagrm.credenciales.user.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController
public class PlatformController {
 private final PlatformService service;private final AppUserRepository users;private final PasswordEncoder encoder;private final JwtService jwt;
 public PlatformController(PlatformService service,AppUserRepository users,PasswordEncoder encoder,JwtService jwt){this.service=service;this.users=users;this.encoder=encoder;this.jwt=jwt;}
 public record Login(@NotBlank @Email String email,@NotBlank String password){}
 public record StatusChange(@NotNull OrganizationStatus status,@NotBlank @Size(max=500) String reason){}
 @PostMapping("/api/auth/platform/login")
 public Map<String,Object> login(@Valid @RequestBody Login req){
  var user=users.findByTenantIsNullAndEmail(req.email().trim().toLowerCase(Locale.ROOT)).filter(u->u.getRole()==UserRole.SUPER_ADMIN&&u.isActive())
    .orElseThrow(()->new BadCredentialsException("Credenciales inválidas"));
  if(!encoder.matches(req.password(),user.getPasswordHash()))throw new BadCredentialsException("Credenciales inválidas");
  return Map.of("token",jwt.create(UserPrincipal.from(user)),"tokenType","Bearer","role","SUPER_ADMIN","email",user.getEmail());
 }
 @GetMapping("/api/platform/organizations") @PreAuthorize("hasRole('SUPER_ADMIN')")
 public List<PlatformService.Organization> list(){return service.list();}
 @GetMapping("/api/institution/status") @PreAuthorize("hasAuthority('ORGANIZATION_VIEW')")
 public PlatformService.Organization own(@AuthenticationPrincipal UserPrincipal p){return service.get(p.tenantId());}
 @GetMapping("/api/platform/organizations/{id}") @PreAuthorize("hasRole('SUPER_ADMIN')")
 public PlatformService.Organization get(@PathVariable UUID id){return service.get(id);}
 @PatchMapping("/api/platform/organizations/{id}/status") @PreAuthorize("hasRole('SUPER_ADMIN')")
 public PlatformService.Organization status(@AuthenticationPrincipal UserPrincipal p,@PathVariable UUID id,@Valid @RequestBody StatusChange req){return service.change(id,p.userId(),req.status(),req.reason());}
 @GetMapping("/api/platform/organizations/{id}/history") @PreAuthorize("hasRole('SUPER_ADMIN')")
 public List<OrganizationStatusEvent> history(@PathVariable UUID id){return service.history(id);}
}
