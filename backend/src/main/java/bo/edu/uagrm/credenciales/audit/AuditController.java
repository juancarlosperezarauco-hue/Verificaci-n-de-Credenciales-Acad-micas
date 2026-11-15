package bo.edu.uagrm.credenciales.audit;

import bo.edu.uagrm.credenciales.security.UserPrincipal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuditController {
 private final AuditQuery query;
 public AuditController(AuditQuery query){this.query=query;}
 @GetMapping("/api/audit/tenant") @PreAuthorize("hasAuthority('AUDIT_VIEW') and hasAnyRole('ADMIN','TENANT_ADMIN','CUSTOM','AUDITOR')")
 public AuditQuery.Page tenant(@AuthenticationPrincipal UserPrincipal p,@RequestParam(required=false) AuditAction action,@RequestParam(required=false) UUID actorId,@RequestParam(required=false) UUID resourceId,@RequestParam(required=false) Instant from,@RequestParam(required=false) Instant before,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return query.search(false,p.tenantId(),action,actorId,resourceId,from,before,page,size);}
 @GetMapping("/api/platform/audit") @PreAuthorize("hasRole('SUPER_ADMIN')")
 public AuditQuery.Page global(@RequestParam(required=false) UUID tenantId,@RequestParam(required=false) AuditAction action,@RequestParam(required=false) UUID actorId,@RequestParam(required=false) UUID resourceId,@RequestParam(required=false) Instant from,@RequestParam(required=false) Instant before,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return query.search(true,tenantId,action,actorId,resourceId,from,before,page,size);}
}
