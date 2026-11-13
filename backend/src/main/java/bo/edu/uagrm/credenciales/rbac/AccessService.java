package bo.edu.uagrm.credenciales.rbac;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.user.AppUser;
import bo.edu.uagrm.credenciales.platform.GovernanceRepository;
import bo.edu.uagrm.credenciales.platform.OrganizationGovernance;
import bo.edu.uagrm.credenciales.platform.OrganizationStatus;
import bo.edu.uagrm.credenciales.user.UserRole;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AccessService {
 private final RoleRepository roles; private final UserRoleRepository assignments; private final RolePermissionRepository grants;
 private final GovernanceRepository governance;
 public AccessService(RoleRepository roles,UserRoleRepository assignments,RolePermissionRepository grants,GovernanceRepository governance){this.roles=roles;this.assignments=assignments;this.grants=grants;this.governance=governance;}
 @Transactional(readOnly=true)
 public UserPrincipal principal(AppUser user){
  var base=UserPrincipal.from(user);
  if(user.getTenant()!=null){
   var status=governance.findById(user.getTenant().getId()).map(OrganizationGovernance::getStatus)
     .orElse(user.getTenant().isActive()?OrganizationStatus.ACTIVE:OrganizationStatus.INACTIVE);
   boolean reviewing=status==OrganizationStatus.PENDING_REVIEW||status==OrganizationStatus.OBSERVED;
   boolean administrator=user.getRole()==UserRole.TENANT_ADMIN||user.getRole()==UserRole.ADMIN;
   if(reviewing&&administrator){
    return new UserPrincipal(base.userId(),base.tenantId(),base.email(),base.password(),base.role(),user.isActive(),
      Set.of("ORGANIZATION_VIEW","ORGANIZATION_UPDATE"),true,0);
   }
   if(status!=OrganizationStatus.ACTIVE) return new UserPrincipal(base.userId(),base.tenantId(),base.email(),base.password(),base.role(),false,Set.of());
  }
  Set<String> permissions=new HashSet<>(base.permissions());
  if(user.getTenant()!=null) for(var assignment:assignments.findByTenantIdAndUserId(user.getTenant().getId(),user.getId())){
   if(roles.findByIdAndTenantId(assignment.getRoleId(),user.getTenant().getId()).isPresent())
    grants.findByRoleId(assignment.getRoleId()).forEach(g->permissions.add(g.getPermissionCode()));
  }
  return new UserPrincipal(base.userId(),base.tenantId(),base.email(),base.password(),base.role(),base.enabled(),Set.copyOf(permissions));
 }
}
