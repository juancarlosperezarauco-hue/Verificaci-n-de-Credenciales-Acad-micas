package bo.edu.uagrm.credenciales.security;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/platform/security/maintenance") @PreAuthorize("hasRole('SUPER_ADMIN')")
public class SecurityMaintenanceController {
 private final SecurityMaintenance service;
 public SecurityMaintenanceController(SecurityMaintenance service){this.service=service;}
 @GetMapping public SecurityMaintenance.Status status(){return service.status();}
 @PostMapping("/re-encrypt") public SecurityMaintenance.Reencrypted reEncrypt(){return service.reEncrypt();}
 @PostMapping("/cleanup") public SecurityMaintenance.Cleaned cleanup(){return service.cleanup();}
}
