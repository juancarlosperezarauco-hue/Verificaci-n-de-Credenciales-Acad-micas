package bo.edu.uagrm.credenciales.portal;

import bo.edu.uagrm.credenciales.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal/items")
@PreAuthorize("hasAuthority('ORGANIZATION_VIEW')")
public class PortalController {
    private final PortalService service;

    public PortalController(PortalService service) { this.service = service; }

    @GetMapping
    List<PortalDtos.Response> list(@AuthenticationPrincipal UserPrincipal principal) {
        return service.list(principal.tenantId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE')")
    PortalDtos.Response create(@AuthenticationPrincipal UserPrincipal principal,
                               @Valid @RequestBody PortalDtos.CreateRequest request) {
        return service.create(principal.tenantId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE')")
    void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        service.delete(principal.tenantId(), id);
    }
}
