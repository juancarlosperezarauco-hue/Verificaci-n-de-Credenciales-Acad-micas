package bo.edu.uagrm.credenciales.credential;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/credentials")
public class PublicVerificationController {
    private final CredentialService credentialService;

    public PublicVerificationController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @GetMapping("/{code}")
    CredentialDtos.PublicVerification verify(@PathVariable String code) {
        return credentialService.verify(code);
    }
    @org.springframework.web.bind.annotation.PostMapping(value="/{code}/check-document",consumes="multipart/form-data")
    CredentialService.DocumentCheck check(@PathVariable String code,@org.springframework.web.bind.annotation.RequestPart org.springframework.web.multipart.MultipartFile file){return credentialService.checkDocument(code,file);}
}
