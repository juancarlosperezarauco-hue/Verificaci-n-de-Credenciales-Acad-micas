package bo.edu.uagrm.credenciales.rbac;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RoleRepository extends JpaRepository<Role,UUID> {
 List<Role> findByTenantIdOrderByName(UUID tenantId);
 Optional<Role> findByIdAndTenantId(UUID id,UUID tenantId);
 boolean existsByTenantIdAndNameIgnoreCaseAndIdNot(UUID tenantId,String name,UUID id);
}
