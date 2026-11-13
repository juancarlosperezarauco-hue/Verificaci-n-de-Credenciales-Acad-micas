package bo.edu.uagrm.credenciales.credential;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface CredentialRepository extends JpaRepository<Credential, UUID> {
    List<Credential> findAllByStudentAccountIdOrderByCreatedAtDesc(UUID studentId);
    List<Credential> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<Credential> findByIdAndTenantId(UUID id, UUID tenantId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Credential c where c.id = :id and c.tenant.id = :tenantId")
    Optional<Credential> findLockedByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
    Optional<Credential> findByVerificationCode(String verificationCode);
    boolean existsByVerificationCode(String verificationCode);
}

