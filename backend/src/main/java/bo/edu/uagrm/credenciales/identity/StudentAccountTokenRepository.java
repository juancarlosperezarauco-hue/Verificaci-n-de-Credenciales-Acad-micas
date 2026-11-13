package bo.edu.uagrm.credenciales.identity;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentAccountTokenRepository extends JpaRepository<StudentAccountToken, UUID> {
    Optional<StudentAccountToken> findByTokenHashAndTokenType(String tokenHash, StudentTokenType type);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StudentAccountToken> findByTokenHashAndTokenTypeAndUsedAtIsNull(String tokenHash, StudentTokenType type);

    @Modifying
    @Query("update StudentAccountToken t set t.usedAt = :usedAt where t.student.id = :studentId and t.tokenType = :type and t.usedAt is null")
    void invalidate(@Param("studentId") UUID studentId, @Param("type") StudentTokenType type,
                    @Param("usedAt") Instant usedAt);
}
