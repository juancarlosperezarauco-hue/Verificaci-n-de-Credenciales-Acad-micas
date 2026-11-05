package bo.edu.uagrm.credenciales.tenant;

import bo.edu.uagrm.credenciales.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/institution/profile")
@PreAuthorize("hasAnyRole('ADMIN','ISSUER','AUDITOR')")
public class InstitutionController {
    private final InstitutionService service;

    public InstitutionController(InstitutionService service) {
        this.service = service;
    }

    @GetMapping
    InstitutionDtos.Profile profile(@AuthenticationPrincipal UserPrincipal principal) {
        return service.profile(principal.tenantId());
    }

    @PatchMapping
    @PreAuthorize("hasRole('ADMIN')")
    InstitutionDtos.Profile update(@AuthenticationPrincipal UserPrincipal principal,
                                    @Valid @RequestBody InstitutionDtos.UpdateBranding request) {
        return service.update(principal.tenantId(), request);
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    InstitutionDtos.Profile logo(@AuthenticationPrincipal UserPrincipal principal,
                                  @RequestPart("file") MultipartFile file) {
        return service.uploadLogo(principal.tenantId(), file);
    }

    @PostMapping(value = "/verification-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    InstitutionDtos.Profile verificationDocument(@AuthenticationPrincipal UserPrincipal principal,
                                                  @RequestPart("file") MultipartFile file) {
        return service.uploadVerificationDocument(principal.tenantId(), file);
    }

    @GetMapping("/verification-document")
    ResponseEntity<byte[]> downloadVerificationDocument(@AuthenticationPrincipal UserPrincipal principal) {
        return response(service.verificationDocument(principal.tenantId()));
    }

    private ResponseEntity<byte[]> response(InstitutionDtos.StoredFile file) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.name(), StandardCharsets.UTF_8).build().toString())
                .body(file.content());
    }
}
