package bo.edu.uagrm.credenciales.workflow;

import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.shared.*;
import bo.edu.uagrm.credenciales.template.TemplateService;

import jakarta.validation.Valid;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
public class RequestController {
    private final RequestService service;

    public RequestController(RequestService s) {
        service = s;
    }

    @GetMapping("/api/me/credential-requests")
    @PreAuthorize("hasRole('STUDENT')")
    List<RequestDtos.View> own(@AuthenticationPrincipal UserPrincipal p) {
        return service.own(p.userId());
    }

    @GetMapping("/api/me/academic-records/{id}/templates")
    @PreAuthorize("hasRole('STUDENT')")
    List<TemplateService.View> catalog(
            @AuthenticationPrincipal UserPrincipal p, @PathVariable UUID id) {
        return service.catalog(p.userId(), id);
    }

    @PostMapping("/api/me/credential-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    RequestDtos.View create(
            @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody RequestDtos.Create r) {
        return service.create(p.userId(), r);
    }

    @PostMapping("/api/me/credentials/{id}/corrections")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    RequestDtos.View correction(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable UUID id,
            @Valid @RequestBody RequestDtos.Correction r) {
        return service.correction(p.userId(), id, r);
    }

    @PostMapping("/api/me/credential-requests/{id}/actions")
    @PreAuthorize("hasRole('STUDENT')")
    RequestDtos.View ownAction(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable UUID id,
            @Valid @RequestBody RequestDtos.Action r) {
        return service.actOwn(p.userId(), id, r);
    }

    @PatchMapping("/api/me/credential-requests/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    RequestDtos.View update(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable UUID id,
            @Valid @RequestBody RequestDtos.Update r) {
        return service.updateOwn(p.userId(), id, r);
    }

    @PostMapping(
            value = "/api/me/credential-requests/{id}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    RequestDtos.Attachment upload(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable UUID id,
            @RequestPart MultipartFile file) {
        return service.upload(p.userId(), id, file);
    }

    @DeleteMapping("/api/me/credential-requests/{id}/attachments/{attachment}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('STUDENT')")
    void remove(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable UUID id,
            @PathVariable UUID attachment) {
        service.removeAttachment(p.userId(), id, attachment);
    }

    @GetMapping("/api/me/credential-requests/{id}/attachments/{attachment}")
    @PreAuthorize("hasRole('STUDENT')")
    ResponseEntity<byte[]> ownFile(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable UUID id,
            @PathVariable UUID attachment) {
        return PrivateDownloads.of(service.ownFile(p.userId(), id, attachment));
    }

    @GetMapping("/api/institution/credential-requests")
    @PreAuthorize("hasAuthority('REQUEST_VIEW')")
    ResultPage<RequestDtos.View> inbox(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(required = false) CredentialRequest.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.inbox(p, status, page, size);
    }

    @GetMapping("/api/institution/credential-requests/{id}")
    @PreAuthorize("hasAuthority('REQUEST_VIEW')")
    RequestDtos.View detail(@AuthenticationPrincipal UserPrincipal p, @PathVariable UUID id) {
        return service.detail(p, id);
    }

    @PostMapping("/api/institution/credential-requests/{id}/actions")
    @PreAuthorize("hasAnyAuthority('REQUEST_REVIEW','REQUEST_APPROVE')")
    RequestDtos.View action(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable UUID id,
            @Valid @RequestBody RequestDtos.Action r) {
        return service.act(p, id, r);
    }

    @GetMapping("/api/institution/credential-requests/{id}/attachments/{attachment}")
    @PreAuthorize("hasAuthority('REQUEST_VIEW')")
    ResponseEntity<byte[]> file(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable UUID id,
            @PathVariable UUID attachment) {
        return PrivateDownloads.of(service.institutionFile(p, id, attachment));
    }
}
