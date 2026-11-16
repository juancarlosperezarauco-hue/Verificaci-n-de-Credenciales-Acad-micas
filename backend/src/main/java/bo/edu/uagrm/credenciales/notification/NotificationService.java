package bo.edu.uagrm.credenciales.notification;

import bo.edu.uagrm.credenciales.identity.StudentAccountRepository;
import bo.edu.uagrm.credenciales.mail.MailOutboxStore;
import bo.edu.uagrm.credenciales.shared.ResultPage;
import bo.edu.uagrm.credenciales.shared.TenantAccess;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import java.time.*;
import java.util.*;

@Service
public class NotificationService {
    private final EntityManager em;
    private final StudentAccountRepository students;
    private final MailOutboxStore mail;
    private final Clock clock;
    private final String frontend;

    public NotificationService(
            EntityManager em,
            StudentAccountRepository students,
            MailOutboxStore mail,
            Clock clock,
            @Value("${app.frontend-url:http://localhost:4200}") String frontend) {
        this.em = em;
        this.students = students;
        this.mail = mail;
        this.clock = clock;
        this.frontend = frontend.replaceAll("/+$", "");
    }

    public record View(
            UUID id,
            UUID institutionId,
            String kind,
            UUID resourceId,
            String title,
            Instant createdAt,
            Instant readAt) {}

    public record Inbox(List<View> items, long total, long unread, int page, int size) {}

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(
            UUID student, UUID tenant, String eventKey, String kind, UUID resource, String title) {
        if (student == null) return;
        // Serialize events for the same identity and make retries idempotent.
        var owner = students.findLockedById(student).filter(s -> s.isActive()).orElse(null);
        if (owner == null) return;
        if (em.createQuery(
                                "select count(n) from AcademicNotification n where n.studentId=:s and n.eventKey=:key",
                                Long.class)
                        .setParameter("s", student)
                        .setParameter("key", eventKey)
                        .getSingleResult()
                > 0) return;
        var n = new AcademicNotification();
        n.id = UUID.randomUUID();
        n.studentId = student;
        n.tenantId = tenant;
        n.eventKey = eventKey;
        n.kind = kind;
        n.resourceId = resource;
        n.title = title;
        n.createdAt = clock.instant();
        em.persist(n);
        if (owner.isEmailVerified())
            mail.enqueue(
                    student,
                    null,
                    "ACADEMIC",
                    n.createdAt.plus(Duration.ofDays(7)),
                    owner.getEmail(),
                    "Actualización en tu expediente académico",
                    "Hay una actualización en tu expediente. Inicia sesión para consultar los detalles: "
                            + frontend
                            + "/student/notifications");
    }

    @Transactional(readOnly = true)
    public Inbox inbox(UUID student, boolean unreadOnly, int page, int size) {
        ResultPage.validate(page, size);
        String where = " where n.studentId=:s" + (unreadOnly ? " and n.readAt is null" : "");
        var rows =
                em.createQuery(
                                "select n from AcademicNotification n"
                                        + where
                                        + " order by n.createdAt desc,n.id desc",
                                AcademicNotification.class)
                        .setParameter("s", student)
                        .setFirstResult(Math.multiplyExact(page, size))
                        .setMaxResults(size)
                        .getResultList();
        long total =
                em.createQuery("select count(n) from AcademicNotification n" + where, Long.class)
                        .setParameter("s", student)
                        .getSingleResult();
        long unread =
                em.createQuery(
                                "select count(n) from AcademicNotification n where n.studentId=:s and n.readAt is null",
                                Long.class)
                        .setParameter("s", student)
                        .getSingleResult();
        return new Inbox(
                rows.stream()
                        .map(n ->
                                new View(
                                        n.id,
                                        n.tenantId,
                                        n.kind,
                                        n.resourceId,
                                        n.title,
                                        n.createdAt,
                                        n.readAt))
                        .toList(),
                total,
                unread,
                page,
                size);
    }

    @Transactional
    public void read(UUID student, UUID id) {
        var rows =
                em.createQuery(
                                "select n from AcademicNotification n where n.id=:id and n.studentId=:s",
                                AcademicNotification.class)
                        .setParameter("id", id)
                        .setParameter("s", student)
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .getResultList();
        if (rows.isEmpty()) throw TenantAccess.missing();
        if (rows.getFirst().readAt == null) rows.getFirst().readAt = clock.instant();
    }

    @Transactional
    public void readAll(UUID student) {
        em.createQuery(
                        "update AcademicNotification n set n.readAt=:now where n.studentId=:s and n.readAt is null")
                .setParameter("now", clock.instant())
                .setParameter("s", student)
                .executeUpdate();
    }
}
