package bo.edu.uagrm.credenciales.portal;

import bo.edu.uagrm.credenciales.tenant.Tenant;
import bo.edu.uagrm.credenciales.tenant.TenantRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalService {
    private final PortalItemRepository items;
    private final TenantRepository tenants;

    public PortalService(PortalItemRepository items, TenantRepository tenants) {
        this.items = items;
        this.tenants = tenants;
    }

    @Transactional(readOnly = true)
    public List<PortalDtos.Response> list(UUID tenantId) {
        return items.findAllByTenantIdAndActiveTrueOrderByPriorityDescCreatedAtDesc(tenantId)
                .stream().map(PortalDtos.Response::from).toList();
    }

    @Transactional
    public PortalDtos.Response create(UUID tenantId, PortalDtos.CreateRequest request) {
        Tenant tenant = tenants.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));
        if (request.itemType() == PortalItemType.DEADLINE && request.eventAt() == null) {
            throw new IllegalArgumentException("Una fecha importante debe indicar día y hora");
        }
        PortalItem item = new PortalItem(tenant, request.itemType(), request.title(), request.description(),
                request.eventAt(), request.link(), request.priority());
        return PortalDtos.Response.from(items.save(item));
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        PortalItem item = items.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Contenido no encontrado"));
        item.deactivate();
    }
}
