package bo.edu.uagrm.credenciales.student;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    List<Student> findAllByTenantIdOrderByFullNameAsc(UUID tenantId);
    Optional<Student> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndDocumentNumberIgnoreCase(UUID tenantId, String documentNumber);
}
