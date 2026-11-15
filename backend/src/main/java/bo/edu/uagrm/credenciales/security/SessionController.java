package bo.edu.uagrm.credenciales.security;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/security/sessions")
public class SessionController {
    private final SessionRegistry sessions;
    public SessionController(SessionRegistry sessions){this.sessions=sessions;}
    @GetMapping List<SessionRegistry.View> list(@AuthenticationPrincipal UserPrincipal principal){return sessions.list(principal);}
    @DeleteMapping("/current") @ResponseStatus(HttpStatus.NO_CONTENT)
    void current(@AuthenticationPrincipal UserPrincipal principal){sessions.revoke(principal,principal.sessionId());}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@AuthenticationPrincipal UserPrincipal principal,@PathVariable UUID id){sessions.revoke(principal,id);}
    @PostMapping("/revoke-others") @ResponseStatus(HttpStatus.NO_CONTENT)
    void others(@AuthenticationPrincipal UserPrincipal principal){sessions.revokeOthers(principal);}
}
