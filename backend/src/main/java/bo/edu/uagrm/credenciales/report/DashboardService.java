package bo.edu.uagrm.credenciales.report;

import bo.edu.uagrm.credenciales.credential.*;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.shared.TenantAccess;

import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final JdbcTemplate jdbc;
    private final CredentialRepository credentials;
    private final TenantAccess access;
    private final Clock clock;

    public DashboardService(
            JdbcTemplate jdbc, CredentialRepository credentials, TenantAccess access, Clock clock) {
        this.jdbc = jdbc;
        this.credentials = credentials;
        this.access = access;
        this.clock = clock;
    }

    public record Snapshot(
            Long linkedStudents,
            Long credentials,
            Long issued,
            Long drafts,
            List<CredentialDtos.Response> recent,
            Instant generatedAt) {}

    public Snapshot snapshot(UserPrincipal p) {
        UUID tenant = access.require(p, "ORGANIZATION_VIEW");
        Long students = null, total = null, issued = null, drafts = null;
        List<CredentialDtos.Response> recent = null;
        if (p.permissions().contains("STUDENT_VIEW"))
            students =
                    jdbc.queryForObject(
                            "select count(distinct c.student_id) from student_consents c join student_accounts s on s.id=c.student_id where c.tenant_id=? and c.used_at is not null and c.revoked_at is null and s.active=true",
                            Long.class,
                            tenant);
        if (p.permissions().contains("CREDENTIAL_VIEW")) {
            var groups =
                    jdbc.query(
                            "select status,count(*) from credentials where tenant_id=? group by status",
                            (r, n) -> Map.entry(r.getString(1), r.getLong(2)),
                            tenant);
            var counts = new HashMap<String, Long>();
            groups.forEach(row -> counts.put(row.getKey(), row.getValue()));
            total = counts.values().stream().mapToLong(Long::longValue).sum();
            issued = counts.getOrDefault("ISSUED", 0L);
            drafts = counts.getOrDefault("DRAFT", 0L);
            recent =
                    credentials
                            .findByTenantId(
                                    tenant,
                                    PageRequest.of(
                                            0, 5, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                            .map(CredentialDtos.Response::from)
                            .toList();
        }
        return new Snapshot(students, total, issued, drafts, recent, clock.instant());
    }
}
