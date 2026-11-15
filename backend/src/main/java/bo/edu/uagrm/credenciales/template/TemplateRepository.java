package bo.edu.uagrm.credenciales.template;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface TemplateRepository extends JpaRepository<DocumentTemplate, UUID> {
    List<DocumentTemplate> findAllByTenantIdOrderByCreatedAtDesc(UUID tenant);

    Optional<DocumentTemplate> findByIdAndTenantId(UUID id, UUID tenant);

    List<DocumentTemplate> findAllByFamilyIdOrderByVersionNumberDesc(UUID family);
}
