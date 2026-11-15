package bo.edu.uagrm.credenciales.identityreview;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.shared.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController
public class IdentityCenterController {
 private final IdentityCenterService service;public IdentityCenterController(IdentityCenterService s){service=s;}
 @GetMapping("/api/me/avatar/status") @PreAuthorize("hasRole('STUDENT')") Map<String,Boolean> state(@AuthenticationPrincipal UserPrincipal p){return service.avatarState(p.userId());}
 @GetMapping("/api/me/avatar") @PreAuthorize("hasRole('STUDENT')") ResponseEntity<byte[]> avatar(@AuthenticationPrincipal UserPrincipal p){var file=service.avatar(p.userId());return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(file.contentType())).header("X-Content-Type-Options","nosniff").body(file.content());}
 @PostMapping(value="/api/me/avatar",consumes="multipart/form-data") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('STUDENT')") void avatar(@AuthenticationPrincipal UserPrincipal p,@RequestPart MultipartFile file){service.avatar(p.userId(),file);}
 @DeleteMapping("/api/me/avatar") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('STUDENT')") void removeAvatar(@AuthenticationPrincipal UserPrincipal p){service.removeAvatar(p.userId());}
 @GetMapping("/api/me/identity-disputes") @PreAuthorize("hasRole('STUDENT')") List<IdentityCenterService.Case> own(@AuthenticationPrincipal UserPrincipal p){return service.own(p.userId());}
 @PostMapping(value="/api/me/identity-disputes",consumes="multipart/form-data") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('STUDENT')") IdentityCenterService.Case create(@AuthenticationPrincipal UserPrincipal p,@Valid @ModelAttribute IdentityCenterService.Claim claim,@RequestPart MultipartFile front,@RequestPart MultipartFile back){return service.create(p.userId(),claim,front,back);}
 @PostMapping("/api/me/identity-disputes/{id}/withdraw") @PreAuthorize("hasRole('STUDENT')") IdentityCenterService.Case withdraw(@AuthenticationPrincipal UserPrincipal p,@PathVariable UUID id){return service.withdraw(p.userId(),id);}
 @PostMapping(value="/api/me/identity-disputes/{id}/resubmit",consumes="multipart/form-data") @PreAuthorize("hasRole('STUDENT')") IdentityCenterService.Case resubmit(@AuthenticationPrincipal UserPrincipal p,@PathVariable UUID id,@RequestParam String explanation,@RequestPart MultipartFile front,@RequestPart MultipartFile back){return service.resubmit(p.userId(),id,explanation,front,back);}
 @GetMapping("/api/me/identity-disputes/{id}/files/{part}") @PreAuthorize("hasRole('STUDENT')") ResponseEntity<byte[]> ownFile(@AuthenticationPrincipal UserPrincipal p,@PathVariable UUID id,@PathVariable String part){return PrivateDownloads.of(service.ownFile(p.userId(),id,part));}
 @GetMapping("/api/platform/identity-disputes") @PreAuthorize("hasRole('SUPER_ADMIN')") ResultPage<IdentityCenterService.Case> inbox(@RequestParam(required=false) IdentityDispute.Status status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.inbox(status,page,size);}
 @PostMapping("/api/platform/identity-disputes/{id}/resolution") @PreAuthorize("hasRole('SUPER_ADMIN')") IdentityCenterService.Case resolve(@AuthenticationPrincipal UserPrincipal p,@PathVariable UUID id,@Valid @RequestBody IdentityCenterService.Resolution body){return service.resolve(p,id,body);}
 @GetMapping("/api/platform/identity-disputes/{id}/files/{part}") @PreAuthorize("hasRole('SUPER_ADMIN')") ResponseEntity<byte[]> rootFile(@AuthenticationPrincipal UserPrincipal p,@PathVariable UUID id,@PathVariable String part){return PrivateDownloads.of(service.rootFile(p,id,part));}
}
