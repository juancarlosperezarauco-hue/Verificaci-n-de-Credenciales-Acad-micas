package bo.edu.uagrm.credenciales.rbac;
import bo.edu.uagrm.credenciales.user.*;
import bo.edu.uagrm.credenciales.tenant.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
@Service @Transactional
public class AccessManagementService {
 private final RoleRepository roles; private final RolePermissionRepository grants; private final UserRoleRepository assignments;
 private final AppUserRepository users; private final TenantRepository tenants; private final PasswordEncoder encoder;
 public AccessManagementService(RoleRepository roles,RolePermissionRepository grants,UserRoleRepository assignments,AppUserRepository users,TenantRepository tenants,PasswordEncoder encoder){
 this.roles=roles;this.grants=grants;this.assignments=assignments;this.users=users;this.tenants=tenants;this.encoder=encoder;}
 private Role role(UUID tenant,UUID id){return roles.findByIdAndTenantId(id,tenant).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 public List<AccessDtos.RoleResponse> roles(UUID tenant){return roles.findByTenantIdOrderByName(tenant).stream().map(this::response).toList();}
 private AccessDtos.RoleResponse response(Role r){Set<String> p=new TreeSet<>();grants.findByRoleId(r.getId()).forEach(g->p.add(g.getPermissionCode()));return new AccessDtos.RoleResponse(r.getId(),r.getName(),p);}
 public AccessDtos.RoleResponse saveRole(UUID tenant,UUID id,AccessDtos.RoleRequest req){
  Role r=id==null?new Role(tenant,req.name().trim()):role(tenant,id);
  if(roles.existsByTenantIdAndNameIgnoreCaseAndIdNot(tenant,req.name().trim(),r.getId())) throw new IllegalArgumentException("Ya existe ese rol");
  r.rename(req.name().trim());roles.saveAndFlush(r);grants.deleteByRoleId(r.getId());grants.flush();
  req.permissions().forEach(p->grants.save(new RolePermission(r.getId(),p.name())));return response(r);
 }
 public void deleteRole(UUID tenant,UUID id){Role r=role(tenant,id);assignments.deleteByRoleId(id);grants.deleteByRoleId(id);roles.delete(r);}
 public List<AccessDtos.UserResponse> users(UUID tenant){return users.findByTenantId(tenant).stream().map(this::response).toList();}
 private AccessDtos.UserResponse response(AppUser u){Set<UUID> ids=new HashSet<>();assignments.findByTenantIdAndUserId(u.getTenant().getId(),u.getId()).forEach(a->ids.add(a.getRoleId()));return new AccessDtos.UserResponse(u.getId(),u.getFullName(),u.getEmail(),u.isActive(),u.getRole().name(),ids);}
 private void validateRoles(UUID tenant,Set<UUID> ids){ids.forEach(id->role(tenant,id));}
 private void assign(UUID tenant,AppUser user,Set<UUID> ids){
  assignments.deleteByTenantIdAndUserId(tenant,user.getId());assignments.flush();
  ids.forEach(id->assignments.save(new UserRole(tenant,user.getId(),id)));
 }
 public AccessDtos.UserResponse create(UUID tenant,AccessDtos.CreateUser req){
  validateRoles(tenant,req.roleIds());String email=req.email().trim().toLowerCase(Locale.ROOT);
  if(users.existsByTenantIdAndEmail(tenant,email))throw new IllegalArgumentException("Correo ya registrado");
  var user=new AppUser(tenants.findById(tenant).orElseThrow(),email,encoder.encode(req.password()),bo.edu.uagrm.credenciales.user.UserRole.CUSTOM);
  user.setFullName(req.fullName().trim());users.saveAndFlush(user);assign(tenant,user,req.roleIds());return response(user);
 }
 private AppUser employee(UUID tenant,UUID id){var u=users.findByIdAndTenantId(id,tenant).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(u.getRole()!=bo.edu.uagrm.credenciales.user.UserRole.CUSTOM)throw new IllegalArgumentException("Solo se pueden modificar empleados con roles personalizados");return u;}
 public AccessDtos.UserResponse update(UUID tenant,UUID id,AccessDtos.UpdateUser req){var u=employee(tenant,id);validateRoles(tenant,req.roleIds());u.setFullName(req.fullName().trim());assign(tenant,u,req.roleIds());return response(u);}
 public void disable(UUID tenant,UUID id){employee(tenant,id).disable();}
}
