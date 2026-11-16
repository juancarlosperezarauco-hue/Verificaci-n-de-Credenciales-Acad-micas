package bo.edu.uagrm.credenciales.credential;

import bo.edu.uagrm.credenciales.shared.ResultPage;

import jakarta.persistence.EntityManager;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class StudentHistoryService {
    private final CredentialRepository credentials;
    private final CredentialVerificationService verification;
    private final EntityManager em;

    public StudentHistoryService(
            CredentialRepository credentials,
            CredentialVerificationService verification,
            EntityManager em) {
        this.credentials = credentials;
        this.verification = verification;
        this.em = em;
    }

    public ResultPage<CredentialDtos.PublicVerification> page(
            UUID student, UUID institution, Integer year, String type, int page, int size) {
        ResultPage.validate(page, size);
        var filter = new CredentialFilters(year, type, null);
        Specification<Credential> specification =
                (root, query, cb) -> {
                    var conditions = new ArrayList<jakarta.persistence.criteria.Predicate>();
                    conditions.add(cb.equal(root.get("studentAccount").get("id"), student));
                    conditions.add(cb.notEqual(root.get("status"), CredentialStatus.DRAFT));
                    if (institution != null)
                        conditions.add(cb.equal(root.get("tenant").get("id"), institution));
                    if (filter.type() != null)
                        conditions.add(cb.equal(root.get("credentialType"), filter.type()));
                    if (year != null) {
                        conditions.add(
                                cb.greaterThanOrEqualTo(root.get("issuedAt"), filter.start()));
                        conditions.add(cb.lessThan(root.get("issuedAt"), filter.end()));
                    }
                    return cb.and(
                            conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
                };
        var rows =
                credentials.findAll(
                        specification,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "issuedAt", "id")));
        return new ResultPage<>(
                rows.map(verification::view).toList(), rows.getTotalElements(), page, size);
    }

    public record Institution(UUID id, String name) {}

    public record Filters(
            List<Institution> institutions, List<String> types, List<Integer> years) {}

    public Filters filters(UUID student) {
        var institutions =
                em
                        .createQuery(
                                "select distinct c.tenant.id,c.tenant.name from Credential c where c.studentAccount.id=:s and c.status<>:draft order by c.tenant.name",
                                Object[].class)
                        .setParameter("s", student)
                        .setParameter("draft", CredentialStatus.DRAFT)
                        .getResultList()
                        .stream()
                        .map(row -> new Institution((UUID) row[0], (String) row[1]))
                        .toList();
        var types =
                em.createQuery(
                                "select distinct c.credentialType from Credential c where c.studentAccount.id=:s and c.status<>:draft order by c.credentialType",
                                String.class)
                        .setParameter("s", student)
                        .setParameter("draft", CredentialStatus.DRAFT)
                        .getResultList();
        var years =
                em.createNativeQuery(
                                "select distinct cast(extract(year from issued_at at time zone 'UTC') as integer) as y from credentials where student_account_id=:s and status<>'DRAFT' and issued_at is not null order by y desc",
                                Integer.class)
                        .setParameter("s", student)
                        .getResultList();
        return new Filters(institutions, types, years);
    }
}
