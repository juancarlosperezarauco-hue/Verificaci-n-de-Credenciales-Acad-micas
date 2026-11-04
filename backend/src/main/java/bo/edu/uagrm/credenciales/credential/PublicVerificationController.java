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
}

