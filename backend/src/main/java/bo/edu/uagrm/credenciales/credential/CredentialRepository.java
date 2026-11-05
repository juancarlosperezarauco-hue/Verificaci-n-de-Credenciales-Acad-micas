package bo.edu.uagrm.credenciales.credential;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CredentialRepository extends JpaRepository<Credential, UUID> {
    List<Credential> findAllByStudentAccountIdOrderByCreatedAtDesc(UUID studentId);
    List<Credential> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<Credential> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Credential> findByVerificationCode(String verificationCode);
    boolean existsByVerificationCode(String verificationCode);
}

