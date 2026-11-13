package bo.edu.uagrm.credenciales.platform;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StatusEventRepository extends JpaRepository<OrganizationStatusEvent,UUID>{
 List<OrganizationStatusEvent> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
 Optional<OrganizationStatusEvent> findFirstByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
