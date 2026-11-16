package bo.edu.uagrm.credenciales.rbac;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRoleRepository extends JpaRepository<UserRole,UUID> {
 List<UserRole> findByTenantIdAndUserId(UUID tenantId,UUID userId);
 void deleteByTenantIdAndUserId(UUID tenantId,UUID userId);
 void deleteByRoleId(UUID roleId);
}
