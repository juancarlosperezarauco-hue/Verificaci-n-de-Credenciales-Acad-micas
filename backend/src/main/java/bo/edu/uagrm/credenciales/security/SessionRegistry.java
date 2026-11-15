package bo.edu.uagrm.credenciales.security;

import bo.edu.uagrm.credenciales.user.UserRole;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional
public class SessionRegistry {
    private final LoginSessionRepository sessions;
    public SessionRegistry(LoginSessionRepository sessions) {this.sessions=sessions;}
    public record View(UUID id,String device,Instant createdAt,Instant lastSeenAt,Instant expiresAt,boolean current,boolean mfaVerified) {}

    public UUID open(UserPrincipal principal,Instant expires) {
        LoginSession session=new LoginSession();session.id=UUID.randomUUID();
        if(principal.role()==UserRole.STUDENT)session.studentId=principal.userId();else session.userId=principal.userId();
        session.tenantId=principal.tenantId();session.securityVersion=principal.securityVersion();
        session.mfaVerified=principal.mfaVerified();session.device=device();
        session.createdAt=Instant.now();session.lastSeenAt=session.createdAt;session.expiresAt=expires;
        return sessions.save(session).id;
    }

    @Transactional(readOnly=true)
    public boolean valid(UUID id,UserPrincipal principal) {
        if(id==null)return false;
        return sessions.findById(id).filter(s->owned(s,principal)&&s.revokedAt==null&&s.expiresAt.isAfter(Instant.now())
                &&s.securityVersion==principal.securityVersion()).isPresent();
    }
    @Transactional(readOnly=true)
    public boolean mfaVerified(UUID id) {return sessions.findById(id).map(s->s.mfaVerified).orElse(false);}
    public void touch(UUID id) {
        sessions.touch(id,Instant.now(),Instant.now().minusSeconds(60));
    }
    @Transactional(readOnly=true)
    public List<View> list(UserPrincipal principal) {
        return ownedSessions(principal).stream().filter(s->s.revokedAt==null&&s.expiresAt.isAfter(Instant.now())
                &&s.securityVersion==principal.securityVersion())
                .map(s->new View(s.id,s.device,s.createdAt,s.lastSeenAt,s.expiresAt,s.id.equals(principal.sessionId()),s.mfaVerified)).toList();
    }
    public void revoke(UserPrincipal principal,UUID id) {
        var session=sessions.findById(id).filter(s->owned(s,principal))
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(session.revokedAt==null)session.revokedAt=Instant.now();
    }
    public void revokeOthers(UserPrincipal principal) {
        ownedSessions(principal).stream().filter(s->!s.id.equals(principal.sessionId())&&s.revokedAt==null)
                .forEach(s->s.revokedAt=Instant.now());
    }
    public void revokeUser(UUID id) {sessions.findByUserIdOrderByCreatedAtDesc(id).forEach(s->s.revokedAt=Instant.now());}
    public void revokeStudent(UUID id) {sessions.findByStudentIdOrderByCreatedAtDesc(id).forEach(s->s.revokedAt=Instant.now());}
    private List<LoginSession> ownedSessions(UserPrincipal principal) {
        return principal.role()==UserRole.STUDENT?sessions.findByStudentIdOrderByCreatedAtDesc(principal.userId())
                :sessions.findByUserIdOrderByCreatedAtDesc(principal.userId());
    }
    private boolean owned(LoginSession session,UserPrincipal principal) {
        return principal.role()==UserRole.STUDENT?principal.userId().equals(session.studentId)
                :principal.userId().equals(session.userId)&&Objects.equals(principal.tenantId(),session.tenantId);
    }
    private String device() {
        if(!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes))return "Aplicación";
        String agent=Optional.ofNullable(attributes.getRequest().getHeader("User-Agent")).orElse("");
        String browser=agent.contains("Edg/")?"Edge":agent.contains("Firefox/")?"Firefox":agent.contains("Chrome/")?"Chrome":agent.contains("Safari/")?"Safari":"Aplicación";
        String os=agent.contains("Android")?"Android":agent.contains("iPhone")||agent.contains("iPad")?"iOS":agent.contains("Windows")?"Windows":agent.contains("Macintosh")?"macOS":agent.contains("Linux")?"Linux":"Otro dispositivo";
        return browser+" · "+os;
    }
}
