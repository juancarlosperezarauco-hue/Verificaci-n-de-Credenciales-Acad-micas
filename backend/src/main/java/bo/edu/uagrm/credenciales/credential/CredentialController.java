package bo.edu.uagrm.credenciales.credential;

import bo.edu.uagrm.credenciales.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/credentials")
public class CredentialController {
    private final CredentialService credentialService;

    public CredentialController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @GetMapping
    List<CredentialDtos.Response> list(@AuthenticationPrincipal UserPrincipal principal) {
        return credentialService.list(principal.tenantId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','ISSUER')")
    CredentialDtos.Response create(@AuthenticationPrincipal UserPrincipal principal,
                                   @Valid @RequestBody CredentialDtos.CreateRequest request) {
        return credentialService.create(principal.tenantId(), request);
    }

    @PostMapping(value = "/{id}/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','ISSUER')")
    CredentialDtos.Response attachDocument(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable UUID id,
                                             @RequestPart("file") MultipartFile file) {
        return credentialService.attachDocument(principal.tenantId(), id, file);
    }

    @PatchMapping("/{id}/issue")
    @PreAuthorize("hasAnyRole('ADMIN','ISSUER')")
    CredentialDtos.Response issue(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return credentialService.issue(principal.tenantId(), id);
    }

    @PatchMapping("/{id}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    CredentialDtos.Response revoke(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id,
                                   @Valid @RequestBody CredentialDtos.RevokeRequest request) {
        return credentialService.revoke(principal.tenantId(), id, request.reason());
    }
}

