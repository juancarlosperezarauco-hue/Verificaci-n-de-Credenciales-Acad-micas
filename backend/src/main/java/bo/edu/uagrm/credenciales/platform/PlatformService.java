package bo.edu.uagrm.credenciales.platform;

import bo.edu.uagrm.credenciales.tenant.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PlatformService {
    private final TenantRepository tenants;
    private final GovernanceRepository governance;
    private final StatusEventRepository events;
    private final OrganizationProfileRepository profiles;

    public PlatformService(TenantRepository tenants, GovernanceRepository governance,
                           StatusEventRepository events, OrganizationProfileRepository profiles) {
        this.tenants = tenants;
        this.governance = governance;
        this.events = events;
        this.profiles = profiles;
    }

    public record Organization(UUID id, String name, String slug, InstitutionType institutionType,
                               boolean active, OrganizationStatus status, String reviewNote,
                               List<OrganizationStatus> allowedTransitions) {}

    private Tenant tenant(UUID id) {
        return tenants.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private OrganizationStatus status(Tenant tenant) {
        return governance.findById(tenant.getId()).map(OrganizationGovernance::getStatus)
                .orElse(tenant.isActive() ? OrganizationStatus.ACTIVE : OrganizationStatus.INACTIVE);
    }

    private Organization response(Tenant tenant) {
        OrganizationStatus state = status(tenant);
        String note = events.findFirstByTenantIdOrderByCreatedAtDesc(tenant.getId())
                .map(OrganizationStatusEvent::getReason).orElse(null);
        return new Organization(tenant.getId(), tenant.getName(), tenant.getSlug(), tenant.getInstitutionType(),
                tenant.isActive(), state, note, transitions(state));
    }

    public static List<OrganizationStatus> transitions(OrganizationStatus state) {
        return switch (state) {
            case PENDING_REVIEW -> List.of(OrganizationStatus.ACTIVE, OrganizationStatus.OBSERVED,
                    OrganizationStatus.REJECTED, OrganizationStatus.INACTIVE);
            case OBSERVED -> List.of(OrganizationStatus.PENDING_REVIEW, OrganizationStatus.ACTIVE,
                    OrganizationStatus.REJECTED, OrganizationStatus.INACTIVE);
            case ACTIVE -> List.of(OrganizationStatus.OBSERVED, OrganizationStatus.SUSPENDED, OrganizationStatus.INACTIVE);
            case SUSPENDED -> List.of(OrganizationStatus.ACTIVE, OrganizationStatus.OBSERVED, OrganizationStatus.INACTIVE);
            case REJECTED -> List.of(OrganizationStatus.PENDING_REVIEW, OrganizationStatus.OBSERVED, OrganizationStatus.INACTIVE);
            case INACTIVE -> List.of(OrganizationStatus.PENDING_REVIEW, OrganizationStatus.OBSERVED);
        };
    }

    @Transactional(readOnly = true)
    public List<Organization> list() { return tenants.findAll().stream().map(this::response).toList(); }

    @Transactional(readOnly = true)
    public Organization get(UUID id) { return response(tenant(id)); }

    public Organization change(UUID id, UUID actor, OrganizationStatus next, String reason) {
        Tenant tenant = tenants.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        OrganizationStatus previous = status(tenant);
        if (!transitions(previous).contains(next)) throw new IllegalArgumentException("El cambio de estado no está permitido.");
        if (next == OrganizationStatus.ACTIVE && !profiles.existsById(id)) {
            throw new IllegalArgumentException("La organización debe completar sus datos institucionales antes de ser aprobada.");
        }
        return recordChange(tenant, actor, previous, next, reason);
    }

    public Organization submitReview(UUID id, UUID actor) {
        Tenant tenant = tenants.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        OrganizationStatus previous = status(tenant);
        if (!profiles.existsById(id)) throw new IllegalArgumentException("Completa y guarda los datos institucionales antes de enviar a revisión.");
        if (previous == OrganizationStatus.PENDING_REVIEW) return response(tenant);
        if (previous != OrganizationStatus.OBSERVED) throw new IllegalArgumentException("La organización no admite un reenvío a revisión en su estado actual.");
        return recordChange(tenant, actor, previous, OrganizationStatus.PENDING_REVIEW, "Correcciones enviadas a revisión por el administrador institucional.");
    }

    private Organization recordChange(Tenant tenant, UUID actor, OrganizationStatus previous,
                                      OrganizationStatus next, String reason) {
        var state = governance.findById(tenant.getId())
                .orElseGet(() -> new OrganizationGovernance(tenant.getId(), previous));
        state.change(next);
        governance.save(state);
        tenant.setActive(next == OrganizationStatus.ACTIVE);
        events.saveAndFlush(new OrganizationStatusEvent(tenant.getId(), actor, previous, next, reason.trim()));
        return response(tenant);
    }

    @Transactional(readOnly = true)
    public List<OrganizationStatusEvent> history(UUID id) {
        tenant(id);
        return events.findByTenantIdOrderByCreatedAtDesc(id);
    }
}
