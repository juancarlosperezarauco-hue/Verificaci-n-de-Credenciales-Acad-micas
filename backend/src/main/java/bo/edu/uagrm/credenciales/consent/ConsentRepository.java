package bo.edu.uagrm.credenciales.consent;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface ConsentRepository extends JpaRepository<StudentConsent,UUID> {
    @Modifying
    @Query("update StudentConsent c set c.revokedAt = :now where c.student.id = :student and c.revokedAt is null")
    void revokeAllByStudent(@org.springframework.data.repository.query.Param("student") UUID student,
                           @org.springframework.data.repository.query.Param("now") java.time.Instant now);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StudentConsent> findByTokenHashAndTenantId(String hash, UUID tenantId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StudentConsent> findByIdAndStudentId(UUID id,UUID studentId);
    List<StudentConsent> findAllByStudentIdOrderByCreatedAtDesc(UUID id);
    List<StudentConsent> findAllByTenantIdAndUsedAtIsNotNullAndRevokedAtIsNull(UUID id);
    boolean existsByStudentIdAndTenantIdAndUsedAtIsNotNullAndRevokedAtIsNull(UUID student, UUID tenant);
}
