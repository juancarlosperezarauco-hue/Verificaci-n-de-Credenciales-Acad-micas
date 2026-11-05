package bo.edu.uagrm.credenciales.consent;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface ConsentRepository extends JpaRepository<StudentConsent,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StudentConsent> findByTokenHashAndTenantId(String hash, UUID tenantId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StudentConsent> findByIdAndStudentId(UUID id,UUID studentId);
    List<StudentConsent> findAllByStudentIdOrderByCreatedAtDesc(UUID id);
    List<StudentConsent> findAllByTenantIdAndUsedAtIsNotNullAndRevokedAtIsNull(UUID id);
    boolean existsByStudentIdAndTenantIdAndUsedAtIsNotNullAndRevokedAtIsNull(UUID student, UUID tenant);
}
