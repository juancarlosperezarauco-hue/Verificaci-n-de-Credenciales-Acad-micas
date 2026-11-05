package bo.edu.uagrm.credenciales.portal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortalItemRepository extends JpaRepository<PortalItem, UUID> {
    List<PortalItem> findAllByTenantIdAndActiveTrueOrderByPriorityDescCreatedAtDesc(UUID tenantId);
    Optional<PortalItem> findByIdAndTenantId(UUID id, UUID tenantId);
}
