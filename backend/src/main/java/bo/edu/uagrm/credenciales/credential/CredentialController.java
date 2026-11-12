package bo.edu.uagrm.credenciales.credential;

import bo.edu.uagrm.credenciales.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/credentials")
public class CredentialController {
    private final CredentialService credentialService;

    public CredentialController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CREDENTIAL_VIEW')")
    List<CredentialDtos.Response> list(@AuthenticationPrincipal UserPrincipal principal) {
        return credentialService.list(principal.tenantId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CREDENTIAL_CREATE')")
    CredentialDtos.Response create(@AuthenticationPrincipal UserPrincipal principal,
                                   @Valid @RequestBody CredentialDtos.CreateRequest request) {
        return credentialService.create(principal.tenantId(), request);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CREDENTIAL_CREATE')")
    List<CredentialDtos.Response> createBatch(@AuthenticationPrincipal UserPrincipal principal,
                                              @Valid @RequestBody CredentialDtos.CreateBatchRequest request) {
        return credentialService.createBatch(principal.tenantId(), request);
    }

    @PostMapping(value = "/{id}/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREDENTIAL_CREATE')")
    CredentialDtos.Response attachDocument(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable UUID id,
                                             @RequestPart("file") MultipartFile file) {
        return credentialService.attachDocument(principal.tenantId(), id, file);
    }

    @PostMapping("/{id}/generate-document")
    @PreAuthorize("hasAuthority('CREDENTIAL_CREATE')")
    CredentialDtos.Response generateDocument(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable UUID id) {
        return credentialService.generateDocument(principal.tenantId(), id);
    }

    @GetMapping("/{id}/document")
    @PreAuthorize("hasAuthority('CREDENTIAL_VIEW')")
    ResponseEntity<byte[]> downloadDocument(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable UUID id) {
        var document = credentialService.document(principal.tenantId(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.name(), StandardCharsets.UTF_8).build().toString())
                .body(document.content());
    }

    @PatchMapping("/{id}/issue")
    @PreAuthorize("hasAuthority('CREDENTIAL_ISSUE')")
    CredentialDtos.Response issue(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return credentialService.issue(principal.tenantId(), id);
    }

    @PatchMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('CREDENTIAL_REVOKE')")
    CredentialDtos.Response revoke(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id,
                                   @Valid @RequestBody CredentialDtos.RevokeRequest request) {
        return credentialService.revoke(principal.tenantId(), id, request.reason());
    }
}

