package bo.edu.uagrm.credenciales.rbac;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RolePermissionRepository extends JpaRepository<RolePermission,UUID> {
 List<RolePermission> findByRoleId(UUID roleId);
 void deleteByRoleId(UUID roleId);
}
