package bo.edu.uagrm.credenciales.workflow;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface RequestRepository extends JpaRepository<CredentialRequest, UUID> {
    @Query("select r.studentId from CredentialRequest r where r.id=:id and r.tenantId=:tenant")
    Optional<UUID> owner(@Param("id") UUID id, @Param("tenant") UUID tenant);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CredentialRequest r where r.id=:id")
    Optional<CredentialRequest> locked(@Param("id") UUID id);

    Optional<CredentialRequest> findByIdAndStudentId(UUID id, UUID student);

    List<CredentialRequest> findAllByStudentIdOrderByCreatedAtDesc(UUID student);

    @Query(
            "select r from CredentialRequest r where r.tenantId=:tenant and (:status is null or r.status=:status) and exists(select c.id from StudentConsent c where c.student.id=r.studentId and c.tenant.id=:tenant and c.usedAt is not null and c.revokedAt is null) and exists(select s.id from StudentAccount s where s.id=r.studentId and s.active=true)")
    Page<CredentialRequest> inbox(
            @Param("tenant") UUID tenant,
            @Param("status") CredentialRequest.Status status,
            Pageable page);
}
