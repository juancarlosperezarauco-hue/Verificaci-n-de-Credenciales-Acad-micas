package bo.edu.uagrm.credenciales.identityreview;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

interface IdentityDisputeRepository extends JpaRepository<IdentityDispute, UUID> {
    List<IdentityDispute> findAllByStudentIdOrderByCreatedAtDesc(UUID student);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from IdentityDispute d where d.id=:id")
    Optional<IdentityDispute> locked(@Param("id") UUID id);

    @Query("select d.studentId from IdentityDispute d where d.id=:id")
    Optional<UUID> owner(@Param("id") UUID id);

    @Query("select d from IdentityDispute d where (:status is null or d.status=:status)")
    Page<IdentityDispute> inbox(@Param("status") IdentityDispute.Status status, Pageable page);
}

interface IdentityDisputeEventRepository extends JpaRepository<IdentityDisputeEvent, UUID> {
    List<IdentityDisputeEvent> findAllByDisputeIdOrderByOccurredAtAsc(UUID id);
}
