package bo.edu.uagrm.credenciales.security;
import bo.edu.uagrm.credenciales.auth.AuthDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class MfaController {
    private final MfaService service;
    public MfaController(MfaService service){this.service=service;}
    public record Password(@NotBlank @Size(max=128) String password){@Override public String toString(){return "MfaPassword[redacted]";}}
    public record Code(@NotBlank @Size(max=64) String code){@Override public String toString(){return "MfaCode[redacted]";}}
    public record Disable(@NotBlank @Size(max=128) String password,@NotBlank @Size(max=64) String code){@Override public String toString(){return "MfaDisable[redacted]";}}
    public record Verify(@NotBlank @Size(max=128) String challengeToken,@NotBlank @Size(max=64) String code){@Override public String toString(){return "MfaVerify[redacted]";}}
    @GetMapping("/api/security/mfa") @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','ADMIN')")
    MfaService.Status status(@AuthenticationPrincipal UserPrincipal principal){return service.status(principal.userId());}
    @PostMapping("/api/security/mfa/setup") @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','ADMIN')")
    MfaService.Setup setup(@AuthenticationPrincipal UserPrincipal principal,@Valid @RequestBody Password body){return service.setup(principal,body.password());}
    @PostMapping("/api/security/mfa/confirm") @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','ADMIN')")
    MfaService.Enabled confirm(@AuthenticationPrincipal UserPrincipal principal,@Valid @RequestBody Code body){return service.confirm(principal,body.code());}
    @PostMapping("/api/security/mfa/replacement") @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','ADMIN')")
    MfaService.Setup replacement(@AuthenticationPrincipal UserPrincipal p,@Valid @RequestBody Disable body){return service.replacement(p,body.password(),body.code()).orElseThrow(()->new BadCredentialsException("Código inválido o ya utilizado."));}
    @PostMapping("/api/security/mfa/recovery-codes") @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','ADMIN')")
    MfaService.Enabled regenerate(@AuthenticationPrincipal UserPrincipal p,@Valid @RequestBody Disable body){return service.regenerate(p,body.password(),body.code()).orElseThrow(()->new BadCredentialsException("Código inválido o ya utilizado."));}
    @DeleteMapping("/api/security/mfa/pending") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','ADMIN')")
    void cancel(@AuthenticationPrincipal UserPrincipal p){service.cancelPending(p);}
    @DeleteMapping("/api/security/mfa") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN','ADMIN')")
    void disable(@AuthenticationPrincipal UserPrincipal principal,@Valid @RequestBody Disable body){if(!service.disable(principal,body.password(),body.code()))throw new BadCredentialsException("Código inválido o ya utilizado.");}
    @PostMapping("/api/auth/mfa/verify")
    AuthDtos.AuthResponse verify(@Valid @RequestBody Verify body){return service.verify(body.challengeToken(),body.code()).orElseThrow(()->new BadCredentialsException("Código inválido, vencido o ya utilizado."));}
}
