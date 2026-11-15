package bo.edu.uagrm.credenciales.security;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

interface UserMfaRepository extends JpaRepository<UserMfa,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select m from UserMfa m where m.userId=:id")
    Optional<UserMfa> locked(@Param("id") UUID id);
}
interface MfaChallengeRepository extends JpaRepository<MfaChallenge,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<MfaChallenge> findByTokenHash(String hash);
}
interface MfaRecoveryRepository extends JpaRepository<MfaRecoveryCode,UUID> {
    List<MfaRecoveryCode> findByUserId(UUID id);
    Optional<MfaRecoveryCode> findByUserIdAndCodeHashAndUsedFalse(UUID id,String hash);
}
