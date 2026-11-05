package bo.edu.uagrm.credenciales.tenant;

import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/institutions")
public class PublicInstitutionController {
    private final InstitutionService service;

    public PublicInstitutionController(InstitutionService service) {
        this.service = service;
    }

    @GetMapping("/{id}/logo")
    ResponseEntity<byte[]> logo(@PathVariable UUID id) {
        var file = service.logo(id);
        return ResponseEntity.ok().cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType(file.contentType())).body(file.content());
    }
}
