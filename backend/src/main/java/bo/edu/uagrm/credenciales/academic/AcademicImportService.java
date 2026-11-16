package bo.edu.uagrm.credenciales.academic;

import bo.edu.uagrm.credenciales.audit.*;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.shared.TenantAccess;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

@Service
@Transactional
public class AcademicImportService {
    private final EntityManager em;
    private final AcademicProgramRepository programs;
    private final AcademicRecordRepository records;
    private final AcademicService academic;
    private final TenantAccess access;
    private final ObjectMapper json;
    private final Clock clock;
    private final AuditTrail audit;

    public AcademicImportService(
            EntityManager em,
            AcademicProgramRepository programs,
            AcademicRecordRepository records,
            AcademicService academic,
            TenantAccess access,
            ObjectMapper json,
            Clock clock,
            AuditTrail audit) {
        this.em = em;
        this.programs = programs;
        this.records = records;
        this.academic = academic;
        this.access = access;
        this.json = json;
        this.clock = clock;
        this.audit = audit;
    }

    public record Problem(int row, String code) {}

    public record Preview(UUID batchId, int rows, Instant expiresAt, List<Problem> problems) {}

    public record Applied(UUID batchId, List<UUID> recordIds, Instant appliedAt) {}

    private List<Problem> validate(UUID tenant, List<AcademicDtos.Enroll> rows, boolean lock) {
        if (lock)
            rows.stream()
                    .map(AcademicDtos.Enroll::studentId)
                    .distinct()
                    .sorted()
                    .forEach(s -> access.linked(s, tenant, true));
        var problems = new ArrayList<Problem>();
        var codes = new HashSet<String>();
        var pairs = new HashSet<String>();
        for (int index = 0; index < rows.size(); index++) {
            var row = rows.get(index);
            int line = index + 1;
            try {
                access.linked(row.studentId(), tenant, false);
            } catch (ResponseStatusException denied) {
                problems.add(new Problem(line, "STUDENT_NOT_LINKED"));
            }
            if (programs.findByIdAndTenantId(row.programId(), tenant)
                    .filter(p -> p.active)
                    .isEmpty()) problems.add(new Problem(line, "PROGRAM_UNAVAILABLE"));
            String code = row.registrationCode().strip();
            if (!codes.add(code) || records.existsByTenantIdAndRegistrationCode(tenant, code))
                problems.add(new Problem(line, "REGISTRATION_EXISTS"));
            if (!pairs.add(row.studentId() + ":" + row.programId())
                    || records.existsByTenantIdAndStudentIdAndProgramId(
                            tenant, row.studentId(), row.programId()))
                problems.add(new Problem(line, "ENROLLMENT_EXISTS"));
        }
        return problems;
    }

    public Preview prepare(UserPrincipal p, List<AcademicDtos.Enroll> rows) {
        UUID tenant = access.require(p, "ACADEMIC_MANAGE");
        var problems = validate(tenant, rows, false);
        if (!problems.isEmpty()) return new Preview(null, rows.size(), null, problems);
        var batch = new AcademicImportBatch();
        batch.id = UUID.randomUUID();
        batch.tenantId = tenant;
        batch.creatorId = p.userId();
        batch.createdAt = clock.instant();
        batch.expiresAt = batch.createdAt.plus(Duration.ofHours(24));
        batch.payload = encode(rows);
        em.persist(batch);
        audit.record(
                AuditAction.ACADEMIC_IMPORT_PREPARED,
                tenant,
                AuditTrail.Resource.ACADEMIC,
                batch.id);
        return new Preview(batch.id, rows.size(), batch.expiresAt, List.of());
    }

    public Applied apply(UserPrincipal p, UUID id) {
        UUID tenant = access.require(p, "ACADEMIC_MANAGE");
        var batches =
                em.createQuery(
                                "select b from AcademicImportBatch b where b.id=:id and b.tenantId=:tenant",
                                AcademicImportBatch.class)
                        .setParameter("id", id)
                        .setParameter("tenant", tenant)
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .getResultList();
        if (batches.isEmpty()) throw TenantAccess.missing();
        var b = batches.getFirst();
        if (b.appliedAt != null)
            return new Applied(
                    id, decode(b.result, new TypeReference<List<UUID>>() {}), b.appliedAt);
        if (!b.expiresAt.isAfter(clock.instant())) throw TenantAccess.conflict();
        List<AcademicDtos.Enroll> rows =
                decode(b.payload, new TypeReference<List<AcademicDtos.Enroll>>() {});
        if (!validate(tenant, rows, true).isEmpty()) throw TenantAccess.conflict();
        var ids = new ArrayList<UUID>();
        for (var row : rows) ids.add(academic.enroll(p, row).id());
        b.appliedAt = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        b.result = encode(ids);
        audit.record(
                AuditAction.ACADEMIC_IMPORT_APPLIED, tenant, AuditTrail.Resource.ACADEMIC, b.id);
        return new Applied(b.id, List.copyOf(ids), b.appliedAt);
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se pudo guardar el lote.", e);
        }
    }

    private <T> T decode(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("El lote no se pudo leer.", e);
        }
    }
}
