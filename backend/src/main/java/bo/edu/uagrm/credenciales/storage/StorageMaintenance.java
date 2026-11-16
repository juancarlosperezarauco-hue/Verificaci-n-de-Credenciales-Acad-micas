package bo.edu.uagrm.credenciales.storage;

import bo.edu.uagrm.credenciales.audit.*;
import bo.edu.uagrm.credenciales.identityreview.EvidenceLifecycle;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class StorageMaintenance {
    public record Status(
            boolean encryptionConfigured,
            String keyId,
            int evidenceRetentionDays,
            boolean scheduled,
            long objects,
            long removed,
            long requested,
            long awaitingReencryption) {}

    public record Cleanup(int removed, int failures) {}

    private final JdbcTemplate jdbc;
    private final ManagedStorage storage;
    private final DocumentStoragePort raw;
    private final EvidenceLifecycle lifecycle;
    private final Clock clock;
    private final AuditTrail audit;
    private final boolean enabled;
    private static final UUID POLICY = UUID.fromString("00000000-0000-0000-0000-000000000020");

    public StorageMaintenance(
            JdbcTemplate j,
            ManagedStorage s,
            DocumentStoragePort r,
            EvidenceLifecycle l,
            Clock c,
            AuditTrail a,
            @Value("${app.evidence.cleanup-enabled:false}") boolean enabled) {
        jdbc = j;
        storage = s;
        raw = r;
        lifecycle = l;
        clock = c;
        audit = a;
        this.enabled = enabled;
    }

    @Transactional(readOnly = true)
    public Status status() {
        return new Status(
                storage.configured(),
                storage.keyId(),
                lifecycle.days(),
                enabled,
                count("1=1"),
                count("state='REMOVED'"),
                count("delete_requested_at is not null and state<>'REMOVED'"),
                jdbc.queryForObject(
                        "select count(*) from managed_objects where state='BOUND' and purpose<>'LEGACY_IDENTITY' and key_id<>?",
                        Long.class,
                        storage.keyId()));
    }

    private long count(String trusted) {
        return jdbc.queryForObject(
                "select count(*) from managed_objects where " + trusted, Long.class);
    }

    public Cleanup cleanup() {
        int removed = 0, failures = 0;
        var keys =
                jdbc.queryForList(
                        "select object_key from managed_objects where state<>'REMOVED' and (delete_requested_at is not null or safe_after<?) order by created_at limit 50",
                        String.class,
                        Timestamp.from(clock.instant()));
        for (String key : keys) {
            var rows =
                    jdbc.queryForList(
                            "select * from managed_objects where object_key=? and state<>'REMOVED' for update skip locked",
                            key);
            if (rows.isEmpty()) continue;
            if (references(key) > 0) {
                jdbc.update(
                        "update managed_objects set delete_requested_at=null,safe_after=? where object_key=?",
                        Timestamp.from(clock.instant().plus(Duration.ofHours(24))),
                        key);
                continue;
            }
            var row = rows.getFirst();
            boolean eligible =
                    row.get("delete_requested_at") != null
                            || instant(row.get("safe_after")).isBefore(clock.instant());
            if (!eligible) continue;
            if (!key.startsWith("managed/") && !"LEGACY_IDENTITY".equals(row.get("purpose")))
                continue;
            try {
                raw.delete(key);
                jdbc.update("update managed_objects set state='REMOVED' where object_key=?", key);
                removed++;
            } catch (RuntimeException failure) {
                failures++;
            }
        }
        if (removed > 0)
            audit.record(
                    AuditAction.STORAGE_PURGED,
                    null,
                    AuditTrail.Resource.STORAGE,
                    POLICY,
                    AuditTrail.Change.purged(removed));
        return new Cleanup(removed, failures);
    }

    public int reencrypt() {
        if (!storage.configured()) throw new IllegalStateException("Cifrado no configurado.");
        var keys =
                jdbc.queryForList(
                        "select object_key from managed_objects where state='BOUND' and purpose<>'LEGACY_IDENTITY' and key_id<>? order by created_at limit 50",
                        String.class,
                        storage.keyId());
        int count = 0;
        for (String key : keys) {
            var row =
                    jdbc.queryForList(
                            "select key_id from managed_objects where object_key=? and state='BOUND' for update",
                            key);
            if (row.isEmpty() || storage.keyId().equals(row.getFirst().get("key_id"))) continue;
            raw.store(key, storage.recipher(key, raw.load(key)), "application/octet-stream");
            jdbc.update(
                    "update managed_objects set key_id=? where object_key=?", storage.keyId(), key);
            audit.record(
                    AuditAction.SECRET_REENCRYPTED,
                    null,
                    AuditTrail.Resource.STORAGE,
                    POLICY,
                    AuditTrail.Change.keyIds(
                            (String) row.getFirst().get("key_id"), storage.keyId()));
            count++;
        }
        return count;
    }

    private Instant instant(Object value) {
        if (value instanceof Timestamp t) return t.toInstant();
        if (value instanceof java.time.OffsetDateTime o) return o.toInstant();
        return Instant.parse(value.toString());
    }

    private long references(String key) {
        return jdbc.queryForObject(
                "select sum(n) from (select count(*) n from credentials where document_object_key=? union all select count(*) n from request_attachments where object_key=? union all select count(*) n from identity_review_submissions where front_key=? or back_key=? or photo_key=? union all select count(*) n from student_accounts where photo_object_key=? union all select count(*) n from identity_disputes where front_key=? or back_key=?) refs",
                Long.class,
                Collections.nCopies(8, key).toArray());
    }
}
