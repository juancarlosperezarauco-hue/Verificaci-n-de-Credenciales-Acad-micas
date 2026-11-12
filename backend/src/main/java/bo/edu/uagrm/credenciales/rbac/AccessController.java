package bo.edu.uagrm.credenciales.rbac;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController @RequestMapping("/api/access")
public class AccessController {
 private final AccessManagementService service;
 public AccessController(AccessManagementService service){this.service=service;}
 @GetMapping("/me")
 public Map<String,Object> me(@AuthenticationPrincipal UserPrincipal p){var result=new LinkedHashMap<String,Object>();result.put("userId",p.userId());result.put("tenantId",p.tenantId());result.put("role",p.role());result.put("permissions",p.permissions());return result;}
 @GetMapping("/permissions") @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN') and hasAnyAuthority('ROLE_VIEW','ROLE_CREATE','ROLE_UPDATE')")
 public List<PermissionCode> permissions(){return List.of(PermissionCode.values());}
 @GetMapping("/roles") @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN') and hasAuthority('ROLE_VIEW')")
 public List<AccessDtos.RoleResponse> roles(@AuthenticationPrincipal UserPrincipal p){return service.roles(p.tenantId());}
 @PostMapping("/roles") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN') and hasAuthority('ROLE_CREATE')")
 public AccessDtos.RoleResponse createRole(@AuthenticationPrincipal UserPrincipal p,@Valid @RequestBody AccessDtos.RoleRequest req){return service.saveRole(p.tenantId(),null,req);}
 @PutMapping("/roles/{id}") @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN') and hasAuthority('ROLE_UPDATE')")
 public AccessDtos.RoleResponse updateRole(@AuthenticationPrincipal UserPrincipal p,@PathVariable UUID id,@Valid @RequestBody AccessDtos.RoleRequest req){return service.saveRole(p.tenantId(),id,req);}
 @DeleteMapping("/roles/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN') and hasAuthority('ROLE_DELETE')")
 public void deleteRole(@AuthenticationPrincipal UserPrincipal p,@PathVariable UUID id){service.deleteRole(p.tenantId(),id);}
 @GetMapping("/users") @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN') and hasAuthority('USER_VIEW')")
 public List<AccessDtos.UserResponse> users(@AuthenticationPrincipal UserPrincipal p){return service.users(p.tenantId());}
 @PostMapping("/users") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN') and hasAuthority('USER_CREATE')")
 public AccessDtos.UserResponse createUser(@AuthenticationPrincipal UserPrincipal p,@Valid @RequestBody AccessDtos.CreateUser req){return service.create(p.tenantId(),req);}
 @PutMapping("/users/{id}") @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN') and hasAuthority('USER_UPDATE')")
 public AccessDtos.UserResponse updateUser(@AuthenticationPrincipal UserPrincipal p,@PathVariable UUID id,@Valid @RequestBody AccessDtos.UpdateUser req){return service.update(p.tenantId(),id,req);}
 @PatchMapping("/users/{id}/disable") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN') and hasAuthority('USER_DISABLE')")
 public void disable(@AuthenticationPrincipal UserPrincipal p,@PathVariable UUID id){service.disable(p.tenantId(),id);}
}
