package bo.edu.uagrm.credenciales.template;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TemplateRepository extends JpaRepository<DocumentTemplate,UUID>{
 List<DocumentTemplate> findAllByTenantIdOrderByCreatedAtDesc(UUID tenant);
 Optional<DocumentTemplate> findByIdAndTenantId(UUID id,UUID tenant);
 List<DocumentTemplate> findAllByFamilyIdOrderByVersionNumberDesc(UUID family);
}
