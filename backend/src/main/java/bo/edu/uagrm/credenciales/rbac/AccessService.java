package bo.edu.uagrm.credenciales.rbac;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.user.AppUser;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AccessService {
 private final RoleRepository roles; private final UserRoleRepository assignments; private final RolePermissionRepository grants;
 public AccessService(RoleRepository roles,UserRoleRepository assignments,RolePermissionRepository grants){this.roles=roles;this.assignments=assignments;this.grants=grants;}
 @Transactional(readOnly=true)
 public UserPrincipal principal(AppUser user){
  var base=UserPrincipal.from(user);
  Set<String> permissions=new HashSet<>(base.permissions());
  if(user.getTenant()!=null) for(var assignment:assignments.findByTenantIdAndUserId(user.getTenant().getId(),user.getId())){
   if(roles.findByIdAndTenantId(assignment.getRoleId(),user.getTenant().getId()).isPresent())
    grants.findByRoleId(assignment.getRoleId()).forEach(g->permissions.add(g.getPermissionCode()));
  }
  return new UserPrincipal(base.userId(),base.tenantId(),base.email(),base.password(),base.role(),base.enabled(),Set.copyOf(permissions));
 }
}
