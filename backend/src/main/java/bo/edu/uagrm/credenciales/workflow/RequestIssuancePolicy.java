package bo.edu.uagrm.credenciales.workflow;

import bo.edu.uagrm.credenciales.academic.*;
import bo.edu.uagrm.credenciales.audit.*;
import bo.edu.uagrm.credenciales.credential.*;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.shared.TenantAccess;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.*;

@Service
public class RequestIssuancePolicy {
    public void lockOwner(UUID tenant, UUID credential) {
        credentials.studentOwner(credential, tenant).ifPresent(id -> access.own(id, true));
    }

    private final RequestRepository requests;
    private final RequestEventRepository events;
    private final CredentialRepository credentials;
    private final TenantAccess access;
    private final AcademicService academic;
    private final Clock clock;
    private final AuditTrail audit;
    private final bo.edu.uagrm.credenciales.notification.NotificationService notifications;

    public RequestIssuancePolicy(
            RequestRepository r,
            RequestEventRepository e,
            CredentialRepository c,
            TenantAccess a,
            AcademicService academic,
            Clock clock,
            AuditTrail audit,
            bo.edu.uagrm.credenciales.notification.NotificationService notifications) {
        requests = r;
        events = e;
        credentials = c;
        access = a;
        this.academic = academic;
        this.clock = clock;
        this.audit = audit;
        this.notifications = notifications;
    }

    public void approved(Credential credential) {
        if (credential.getSourceRequestId() == null) return;
        var r = requests.locked(credential.getSourceRequestId()).orElseThrow(TenantAccess::missing);
        if (r.status != CredentialRequest.Status.APPROVED
                || !r.tenantId.equals(credential.getTenant().getId())
                || r.studentRevision != access.own(r.studentId, false).getIdentityRevision())
            throw new IllegalArgumentException(
                    "El trámite no está aprobado o el perfil cambió; inicia una solicitud actualizada.");
    }

    public String programName(Credential c) {
        if (c.getSourceRequestId() == null) return c.getTitle();
        var r = requests.findById(c.getSourceRequestId()).orElseThrow(TenantAccess::missing);
        return academic.programName(academic.snapshot(r.recordId, r.tenantId, r.studentId));
    }

    public void fulfilled(Credential c) {
        if (c.getSourceRequestId() == null) return;
        var r = requests.locked(c.getSourceRequestId()).orElseThrow(TenantAccess::missing);
        if (r.status != CredentialRequest.Status.APPROVED) throw TenantAccess.conflict();
        if (r.sourceCredentialId != null) {
            var old =
                    credentials
                            .findLockedByIdAndTenantId(r.sourceCredentialId, r.tenantId)
                            .orElseThrow(TenantAccess::missing);
            if (old.getStatus() != CredentialStatus.ISSUED
                    || old.getStudentAccount() == null
                    || !old.getStudentAccount().getId().equals(r.studentId))
                throw TenantAccess.conflict();
            old.revoke("Documento sustituido por una corrección aprobada.");
            old.replacedBy(c.getId());
            notifications.publish(
                    r.studentId,
                    r.tenantId,
                    "credential:" + old.getId() + ":REVOKED",
                    "CREDENTIAL_REVOKED",
                    old.getId(),
                    "Una credencial fue sustituida por una corrección");
            c.replaces(old.getId());
            audit.record(
                    AuditAction.CREDENTIAL_REPLACED,
                    r.tenantId,
                    AuditTrail.Resource.CREDENTIAL,
                    old.getId());
        }
        r.status = CredentialRequest.Status.FULFILLED;
        r.updatedAt = clock.instant();
        var event = new RequestEvent();
        event.id = UUID.randomUUID();
        event.requestId = r.id;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        event.actorId =
                auth != null && auth.getPrincipal() instanceof UserPrincipal p
                        ? p.userId()
                        : r.approverId;
        event.action = "ISSUE";
        event.status = r.status.name();
        event.occurredAt = r.updatedAt;
        events.save(event);
        audit.record(
                AuditAction.REQUEST_TRANSITIONED, r.tenantId, AuditTrail.Resource.REQUEST, r.id);
    }
}
