package bo.edu.uagrm.credenciales.security;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

interface LoginSessionRepository extends JpaRepository<LoginSession,UUID> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update LoginSession s set s.lastSeenAt=:now where s.id=:id and s.revokedAt is null and s.lastSeenAt<:cutoff")
    int touch(@org.springframework.data.repository.query.Param("id") UUID id,@org.springframework.data.repository.query.Param("now") java.time.Instant now,
              @org.springframework.data.repository.query.Param("cutoff") java.time.Instant cutoff);
    List<LoginSession> findByUserIdOrderByCreatedAtDesc(UUID user);
    List<LoginSession> findByStudentIdOrderByCreatedAtDesc(UUID student);
}
