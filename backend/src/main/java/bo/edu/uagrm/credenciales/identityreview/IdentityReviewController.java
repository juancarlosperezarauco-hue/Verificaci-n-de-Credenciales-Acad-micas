package bo.edu.uagrm.credenciales.identityreview;

import bo.edu.uagrm.credenciales.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class IdentityReviewController {
    private final IdentityReviewService service;
    public IdentityReviewController(IdentityReviewService service){this.service=service;}
    @GetMapping("/api/me/identity-verifications") @PreAuthorize("hasRole('STUDENT')")
    IdentityReviewDtos.Own own(@AuthenticationPrincipal UserPrincipal p){return service.own(p.userId());}
    @PostMapping(value="/api/me/identity-verifications",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('STUDENT')")
    IdentityReviewDtos.Item submit(@AuthenticationPrincipal UserPrincipal p,@RequestParam UUID tenantId,
        @RequestParam boolean accepted,@RequestPart MultipartFile front,@RequestPart MultipartFile back,
        @RequestPart(required=false) MultipartFile photo){return service.submit(p.userId(),tenantId,accepted,front,back,photo);}
    @PostMapping("/api/me/identity-verifications/{id}/withdraw") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('STUDENT')")
    void withdraw(@AuthenticationPrincipal UserPrincipal p,@PathVariable UUID id){service.withdraw(p.userId(),id);}
    @GetMapping("/api/me/identity-verifications/{id}/files/{part}") @PreAuthorize("hasRole('STUDENT')")
    ResponseEntity<byte[]> ownFile(@AuthenticationPrincipal UserPrincipal p,@PathVariable UUID id,@PathVariable String part){return image(service.ownFile(p.userId(),id,part));}
    @GetMapping("/api/institution/identity-verifications") @PreAuthorize("hasAuthority('IDENTITY_REVIEW')")
    IdentityReviewDtos.Results inbox(@AuthenticationPrincipal UserPrincipal p,@RequestParam(required=false) IdentitySubmission.Status status,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.inbox(p,status,page,size);}
    @GetMapping("/api/institution/identity-verifications/{id}") @PreAuthorize("hasAuthority('IDENTITY_REVIEW')")
    IdentityReviewDtos.Item detail(@AuthenticationPrincipal UserPrincipal p,@PathVariable UUID id){return service.detail(p,id);}
    @PostMapping("/api/institution/identity-verifications/{id}/decision") @PreAuthorize("hasAuthority('IDENTITY_REVIEW')")
    IdentityReviewDtos.Item decide(@AuthenticationPrincipal UserPrincipal p,@PathVariable UUID id,@Valid @RequestBody IdentityReviewDtos.Decision body){return service.decide(p,id,body);}
    @GetMapping("/api/institution/identity-verifications/{id}/files/{part}") @PreAuthorize("hasAuthority('IDENTITY_REVIEW')")
    ResponseEntity<byte[]> file(@AuthenticationPrincipal UserPrincipal p,@PathVariable UUID id,@PathVariable String part){return image(service.institutionFile(p,id,part));}
    private ResponseEntity<byte[]> image(byte[] bytes){return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CacheControl.noStore())
        .header("X-Content-Type-Options","nosniff").header("Content-Disposition","inline; filename=\"identidad.jpg\"")
        .header("Referrer-Policy","no-referrer").body(bytes);}
}
