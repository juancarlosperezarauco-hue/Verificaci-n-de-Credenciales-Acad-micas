package bo.edu.uagrm.credenciales.credential;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CredentialRepository extends JpaRepository<Credential, UUID> {
    List<Credential> findAllByStudentAccountIdOrderByCreatedAtDesc(UUID studentId);

    List<Credential> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<Credential> findByIdAndTenantId(UUID id, UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Credential c where c.id = :id and c.tenant.id = :tenantId")
    Optional<Credential> findLockedByIdAndTenantId(
            @Param("id") UUID id, @Param("tenantId") UUID tenantId);

    Optional<Credential> findByVerificationCode(String verificationCode);

    boolean existsByVerificationCode(String verificationCode);

    Optional<Credential> findBySourceRequestId(UUID requestId);

    @Query("select c.studentAccount.id from Credential c where c.id=:id and c.tenant.id=:tenant")
    Optional<UUID> studentOwner(@Param("id") UUID id, @Param("tenant") UUID tenant);
}
