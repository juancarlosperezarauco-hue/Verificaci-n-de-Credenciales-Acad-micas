package bo.edu.uagrm.credenciales.identityreview;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface IdentitySubmissionRepository extends JpaRepository<IdentitySubmission,UUID> {
    List<IdentitySubmission> findAllByStudentIdOrderBySubmittedAtDesc(UUID studentId);
    List<IdentitySubmission> findAllByStudentIdAndTenantIdAndWithdrawnAtIsNullOrderBySubmittedAtDesc(UUID studentId,UUID tenantId);
    Optional<IdentitySubmission> findByIdAndStudentId(UUID id,UUID studentId);
    Optional<IdentitySubmission> findByIdAndTenantId(UUID id,UUID tenantId);
    @Query("select s.studentId from IdentitySubmission s where s.id=:id and s.tenantId=:tenant")
    Optional<UUID> owner(@Param("id") UUID id,@Param("tenant") UUID tenant);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from IdentitySubmission s where s.id=:id and s.tenantId=:tenant")
    Optional<IdentitySubmission> locked(@Param("id") UUID id,@Param("tenant") UUID tenant);
    @Query("""
        select s from IdentitySubmission s where s.tenantId=:tenant and s.withdrawnAt is null
        and (:status is null or s.status=:status)
        and exists (select a.id from StudentAccount a where a.id=s.studentId and a.active=true)
        and exists (select c.id from StudentConsent c where c.id=s.consentId and c.student.id=s.studentId
            and c.tenant.id=:tenant and c.usedAt is not null and c.revokedAt is null)
        """)
    Page<IdentitySubmission> inbox(@Param("tenant") UUID tenant,@Param("status") IdentitySubmission.Status status,Pageable page);
}
