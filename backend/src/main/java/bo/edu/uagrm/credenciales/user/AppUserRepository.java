package bo.edu.uagrm.credenciales.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    java.util.List<AppUser> findByTenantId(UUID tenantId);
    Optional<AppUser> findByTenantIsNullAndEmail(String email);
    Optional<AppUser> findByIdAndTenantIsNull(UUID id);
    @EntityGraph(attributePaths = "tenant")
    Optional<AppUser> findByTenantIdAndEmail(UUID tenantId, String email);
    @EntityGraph(attributePaths = "tenant")
    Optional<AppUser> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
}

