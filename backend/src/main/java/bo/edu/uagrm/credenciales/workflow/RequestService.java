package bo.edu.uagrm.credenciales.workflow;

import bo.edu.uagrm.credenciales.academic.*;
import bo.edu.uagrm.credenciales.audit.*;
import bo.edu.uagrm.credenciales.credential.*;
import bo.edu.uagrm.credenciales.identityreview.*;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.shared.*;
import bo.edu.uagrm.credenciales.storage.ManagedStorage;
import bo.edu.uagrm.credenciales.template.*;
import bo.edu.uagrm.credenciales.tenant.TenantRepository;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.util.*;

@Service
@Transactional
public class RequestService {
    private final RequestRepository requests;
    private final RequestEventRepository events;
    private final RequestAttachmentRepository attachments;
    private final AcademicService academic;
    private final TenantAccess access;
    private final TenantRepository tenants;
    private final CredentialRepository credentials;
    private final TemplateService templates;
    private final TemplateRepository templateRows;
    private final ManagedStorage storage;
    private final PdfSafety pdf;
    private final IdentityImages images;
    private final AuditTrail audit;
    private final Clock clock;

    public RequestService(
            RequestRepository r,
            RequestEventRepository e,
            RequestAttachmentRepository a,
            AcademicService academic,
            TenantAccess access,
            TenantRepository tenants,
            CredentialRepository credentials,
            TemplateService templates,
            TemplateRepository templateRows,
            ManagedStorage storage,
            PdfSafety pdf,
            IdentityImages images,
            AuditTrail audit,
            Clock clock) {
        requests = r;
        events = e;
        attachments = a;
        this.academic = academic;
        this.access = access;
        this.tenants = tenants;
        this.credentials = credentials;
        this.templates = templates;
        this.templateRows = templateRows;
        this.storage = storage;
        this.pdf = pdf;
        this.images = images;
        this.audit = audit;
        this.clock = clock;
    }

    public RequestDtos.View create(UUID student, RequestDtos.Create input) {
        return create(student, input, null);
    }

    private RequestDtos.View create(UUID student, RequestDtos.Create input, Credential original) {
        var own = access.own(student, true);
        if (!own.isProfileComplete() || !own.isEmailVerified())
            throw new IllegalArgumentException(
                    "Completa tu perfil y confirma el correo antes de solicitar un documento.");
        var record =
                academic.own(student).stream()
                        .filter(r -> r.id().equals(input.recordId()))
                        .findFirst()
                        .orElseThrow(TenantAccess::missing);
        academic.require(record.id(), record.tenantId(), student, true);
        if (original != null
                && (!original.getTenant().getId().equals(record.tenantId())
                        || original.getStudentAccount() == null
                        || !original.getStudentAccount().getId().equals(student)
                        || original.getStatus() != CredentialStatus.ISSUED))
            throw TenantAccess.missing();
        var terminal =
                Set.of(
                        CredentialRequest.Status.FULFILLED,
                        CredentialRequest.Status.REJECTED,
                        CredentialRequest.Status.WITHDRAWN);
        boolean duplicate =
                requests.findAllByStudentIdOrderByCreatedAtDesc(student).stream()
                        .anyMatch(r ->
                                !terminal.contains(r.status)
                                        && (original == null
                                                ? r.sourceCredentialId == null
                                                        && r.recordId.equals(record.id())
                                                        && r.credentialType.equals(
                                                                input.credentialType())
                                                : original.getId().equals(r.sourceCredentialId)));
        if (duplicate) throw TenantAccess.conflict();
        if (input.templateVersionId() != null)
            templates.require(
                    record.tenantId(), input.templateVersionId(), input.credentialType(), true);
        var row = new CredentialRequest();
        row.id = UUID.randomUUID();
        row.tenantId = record.tenantId();
        row.studentId = student;
        row.recordId = record.id();
        row.credentialType = input.credentialType();
        row.title = input.title().strip();
        row.message = input.message();
        row.templateVersionId = input.templateVersionId();
        row.studentRevision = own.getIdentityRevision();
        row.sourceCredentialId = original == null ? null : original.getId();
        row.status = CredentialRequest.Status.SUBMITTED;
        row.createdAt = clock.instant();
        row.updatedAt = row.createdAt;
        requests.saveAndFlush(row);
        event(row, student, "SUBMIT", null);
        audit.record(
                AuditAction.REQUEST_CREATED, row.tenantId, AuditTrail.Resource.REQUEST, row.id);
        return view(row);
    }

    public RequestDtos.View correction(
            UUID student, UUID originalId, RequestDtos.Correction input) {
        var original =
                credentials
                        .findById(originalId)
                        .filter(c ->
                                c.getStudentAccount() != null
                                        && c.getStudentAccount().getId().equals(student))
                        .orElseThrow(TenantAccess::missing);
        return create(
                student,
                new RequestDtos.Create(
                        input.recordId(),
                        original.getCredentialType(),
                        input.title(),
                        input.reason(),
                        input.templateVersionId()),
                original);
    }

    @Transactional(readOnly = true)
    public List<TemplateService.View> catalog(UUID student, UUID recordId) {
        var record =
                academic.own(student).stream()
                        .filter(r -> r.id().equals(recordId))
                        .findFirst()
                        .orElseThrow(TenantAccess::missing);
        access.linked(student, record.tenantId(), false);
        return templateRows.findAllByTenantIdOrderByCreatedAtDesc(record.tenantId()).stream()
                .filter(DocumentTemplate::isActive)
                .map(templates::view)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RequestDtos.View> own(UUID student) {
        access.own(student, false);
        return requests.findAllByStudentIdOrderByCreatedAtDesc(student).stream()
                .map(this::view)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResultPage<RequestDtos.View> inbox(
            UserPrincipal p, CredentialRequest.Status status, int page, int size) {
        ResultPage.validate(page, size);
        var rows =
                requests.inbox(
                        access.require(p, "REQUEST_VIEW"),
                        status,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new ResultPage<>(
                rows.stream().map(this::view).toList(), rows.getTotalElements(), page, size);
    }

    public RequestDtos.View detail(UserPrincipal p, UUID id) {
        return view(institutional(p, id, "REQUEST_VIEW"));
    }

    public RequestDtos.View updateOwn(UUID student, UUID id, RequestDtos.Update input) {
        var row = ownLocked(student, id);
        access.linked(student, row.tenantId, true);
        editable(row);
        if (input.templateVersionId() != null
                && !Objects.equals(input.templateVersionId(), row.templateVersionId))
            templates.require(row.tenantId, input.templateVersionId(), row.credentialType, true);
        row.title = input.title().strip();
        row.message = input.message();
        row.templateVersionId = input.templateVersionId();
        row.studentRevision = access.own(student, false).getIdentityRevision();
        event(row, student, "EDIT", null);
        return view(row);
    }

    public RequestDtos.View actOwn(UUID student, UUID id, RequestDtos.Action action) {
        var row = ownLocked(student, id);
        if (action.action().equals("WITHDRAW")) {
            if (Set.of(
                            CredentialRequest.Status.FULFILLED,
                            CredentialRequest.Status.REJECTED,
                            CredentialRequest.Status.WITHDRAWN)
                    .contains(row.status)) throw TenantAccess.conflict();
            row.status = CredentialRequest.Status.WITHDRAWN;
        } else if (action.action().equals("RESUBMIT")) {
            if (row.status != CredentialRequest.Status.OBSERVED) throw TenantAccess.conflict();
            access.linked(student, row.tenantId, true);
            row.status = CredentialRequest.Status.SUBMITTED;
            row.reviewerId = null;
            row.approverId = null;
            row.studentRevision = access.own(student, false).getIdentityRevision();
        } else throw new IllegalArgumentException("Acción estudiantil no permitida.");
        event(row, student, action.action(), action.reason());
        return view(row);
    }

    public RequestDtos.View act(UserPrincipal p, UUID id, RequestDtos.Action action) {
        String permission =
                action.action().equals("APPROVE") ? "REQUEST_APPROVE" : "REQUEST_REVIEW";
        var row = institutional(p, id, permission);
        if (Set.of("OBSERVE", "REJECT").contains(action.action())
                && (action.reason() == null || action.reason().strip().length() < 10))
            throw new IllegalArgumentException("Explica la decisión con al menos 10 caracteres.");
        switch (action.action()) {
            case "START" -> {
                state(row, CredentialRequest.Status.SUBMITTED);
                row.status = CredentialRequest.Status.IN_REVIEW;
                row.reviewerId = p.userId();
            }
            case "OBSERVE" -> {
                reviewer(row, p);
                row.status = CredentialRequest.Status.OBSERVED;
            }
            case "RECOMMEND" -> {
                reviewer(row, p);
                if (changed(row)) throw TenantAccess.conflict();
                row.status = CredentialRequest.Status.REVIEWED;
            }
            case "REJECT" -> {
                if (!Set.of(
                                CredentialRequest.Status.SUBMITTED,
                                CredentialRequest.Status.IN_REVIEW,
                                CredentialRequest.Status.REVIEWED)
                        .contains(row.status)) throw TenantAccess.conflict();
                row.status = CredentialRequest.Status.REJECTED;
            }
            case "APPROVE" -> {
                state(row, CredentialRequest.Status.REVIEWED);
                if (p.userId().equals(row.reviewerId))
                    throw new IllegalArgumentException(
                            "La aprobación debe realizarla una persona distinta al revisor.");
                if (changed(row)) throw TenantAccess.conflict();
                row.approverId = p.userId();
                row.status = CredentialRequest.Status.APPROVED;
                var credential =
                        new Credential(
                                access.active(row.tenantId),
                                access.linked(row.studentId, row.tenantId, false),
                                row.credentialType,
                                row.title);
                credential.configureRequest(row.id, row.templateVersionId);
                credentials.saveAndFlush(credential);
                audit.record(
                        AuditAction.CREDENTIAL_CREATED,
                        row.tenantId,
                        AuditTrail.Resource.CREDENTIAL,
                        credential.getId());
            }
            default -> throw new IllegalArgumentException("Acción institucional no permitida.");
        }
        event(row, p.userId(), action.action(), action.reason());
        return view(row);
    }

    public RequestDtos.Attachment upload(UUID student, UUID id, MultipartFile file) {
        var row = ownLocked(student, id);
        access.linked(student, row.tenantId, true);
        editable(row);
        if (attachments.countByRequestId(id) >= 5)
            throw new IllegalArgumentException("Máximo cinco adjuntos por solicitud.");
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
            throw new IllegalArgumentException("Adjunta PDF, JPEG o PNG de hasta 5 MiB.");
        try {
            byte[] content = file.getBytes();
            String type;
            if (content.length > 4 && content[0] == '%' && content[1] == 'P') {
                pdf.validate(content);
                type = "application/pdf";
            } else {
                content = images.normalize(file);
                type = "image/jpeg";
            }
            var a = new RequestAttachment();
            a.id = UUID.randomUUID();
            a.requestId = id;
            a.contentHash = IdentityImages.hash(content);
            a.contentType = type;
            a.name =
                    "requisito-"
                            + a.id.toString().substring(0, 8)
                            + (type.equals("application/pdf") ? ".pdf" : ".jpg");
            a.createdAt = clock.instant();
            a.objectKey = storage.store(row.tenantId, a.id, "REQUEST_ATTACHMENT", content, type);
            attachments.save(a);
            audit.record(
                    AuditAction.REQUEST_ATTACHMENT_ADDED,
                    row.tenantId,
                    AuditTrail.Resource.REQUEST,
                    id);
            return attachment(a);
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("No se pudo leer el adjunto.");
        }
    }

    public void removeAttachment(UUID student, UUID id, UUID attachment) {
        var row = ownLocked(student, id);
        editable(row);
        var a = attachments.findByIdAndRequestId(attachment, id).orElseThrow(TenantAccess::missing);
        attachments.delete(a);
        storage.requestDeletion(a.objectKey);
    }

    public CredentialDtos.StoredDocument ownFile(UUID student, UUID id, UUID attachment) {
        ownLocked(student, id);
        return file(id, attachment);
    }

    public CredentialDtos.StoredDocument institutionFile(
            UserPrincipal p, UUID id, UUID attachment) {
        institutional(p, id, "REQUEST_VIEW");
        return file(id, attachment);
    }

    private CredentialDtos.StoredDocument file(UUID id, UUID attachment) {
        var a = attachments.findByIdAndRequestId(attachment, id).orElseThrow(TenantAccess::missing);
        return new CredentialDtos.StoredDocument(storage.load(a.objectKey), a.contentType, a.name);
    }

    private CredentialRequest ownLocked(UUID student, UUID id) {
        access.own(student, true);
        var row =
                requests.locked(id)
                        .filter(r -> r.studentId.equals(student))
                        .orElseThrow(TenantAccess::missing);
        return row;
    }

    private CredentialRequest institutional(UserPrincipal p, UUID id, String permission) {
        UUID tenant = access.require(p, permission);
        UUID student = requests.owner(id, tenant).orElseThrow(TenantAccess::missing);
        access.linked(student, tenant, true);
        return requests.locked(id)
                .filter(r -> r.tenantId.equals(tenant))
                .orElseThrow(TenantAccess::missing);
    }

    private boolean changed(CredentialRequest row) {
        return row.studentRevision != access.own(row.studentId, false).getIdentityRevision();
    }

    private void state(CredentialRequest r, CredentialRequest.Status expected) {
        if (r.status != expected) throw TenantAccess.conflict();
    }

    private void reviewer(CredentialRequest r, UserPrincipal p) {
        state(r, CredentialRequest.Status.IN_REVIEW);
        if (!p.userId().equals(r.reviewerId))
            throw new IllegalArgumentException(
                    "Solo el revisor asignado puede completar esta revisión.");
    }

    private void editable(CredentialRequest r) {
        if (!Set.of(CredentialRequest.Status.SUBMITTED, CredentialRequest.Status.OBSERVED)
                .contains(r.status)) throw TenantAccess.conflict();
    }

    void event(CredentialRequest r, UUID actor, String action, String reason) {
        var e = new RequestEvent();
        e.id = UUID.randomUUID();
        e.requestId = r.id;
        e.actorId = actor;
        e.action = action;
        e.status = r.status.name();
        e.reason = reason;
        e.occurredAt = clock.instant();
        events.save(e);
        r.updatedAt = e.occurredAt;
        audit.record(
                AuditAction.REQUEST_TRANSITIONED, r.tenantId, AuditTrail.Resource.REQUEST, r.id);
    }

    private RequestDtos.Attachment attachment(RequestAttachment a) {
        return new RequestDtos.Attachment(a.id, a.name, a.contentType, a.createdAt);
    }

    private RequestDtos.View view(CredentialRequest r) {
        var record = academic.snapshot(r.recordId, r.tenantId, r.studentId);
        return new RequestDtos.View(
                r.id,
                r.tenantId,
                tenants.findById(r.tenantId).orElseThrow().getName(),
                r.studentId,
                access.own(r.studentId, false).getFullName(),
                r.recordId,
                academic.programName(record),
                r.credentialType,
                r.title,
                r.message,
                r.status,
                r.reviewerId,
                r.approverId,
                r.templateVersionId,
                r.sourceCredentialId,
                credentials.findBySourceRequestId(r.id).map(Credential::getId).orElse(null),
                changed(r),
                r.createdAt,
                r.updatedAt,
                attachments.findAllByRequestIdOrderByCreatedAtAsc(r.id).stream()
                        .map(this::attachment)
                        .toList(),
                events.findAllByRequestIdOrderByOccurredAtAsc(r.id).stream()
                        .map(e ->
                                new RequestDtos.Event(
                                        e.id, e.action, e.status, e.reason, e.occurredAt))
                        .toList());
    }
}
