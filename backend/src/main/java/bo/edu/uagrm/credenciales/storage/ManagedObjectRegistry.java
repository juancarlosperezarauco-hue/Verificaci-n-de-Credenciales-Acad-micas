package bo.edu.uagrm.credenciales.storage;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;

/** Independent reservation survives rollback/crash of the document transaction. */
@Service
public class ManagedObjectRegistry {
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public ManagedObjectRegistry(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserve(
            String key,
            UUID owner,
            UUID tenant,
            String purpose,
            String hash,
            long size,
            String keyId) {
        var now = clock.instant();
        jdbc.update(
                "insert into managed_objects(id,object_key,owner_id,tenant_id,purpose,content_hash,size_bytes,key_id,state,created_at,safe_after) values(?,?,?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID(),
                key,
                owner,
                tenant,
                purpose,
                hash,
                size,
                keyId,
                "STAGED",
                Timestamp.from(now),
                Timestamp.from(now.plus(Duration.ofHours(24))));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void activate(String key) {
        int updated =
                jdbc.update(
                        "update managed_objects set state='BOUND',safe_after=? where object_key=? and state='STAGED'",
                        Timestamp.from(clock.instant().plus(Duration.ofHours(24))),
                        key);
        if (updated != 1)
            throw new IllegalStateException(
                    "La reserva del archivo expiró. Intenta la operación de nuevo.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finish(String key, boolean committed) {
        jdbc.update(
                "update managed_objects set state=?,delete_requested_at=? where object_key=? and state in ('STAGED','BOUND')",
                committed ? "BOUND" : "ABANDONED",
                committed ? null : Timestamp.from(clock.instant()),
                key);
    }
}
