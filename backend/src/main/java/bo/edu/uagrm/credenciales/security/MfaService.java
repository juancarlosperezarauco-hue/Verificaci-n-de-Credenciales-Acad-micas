package bo.edu.uagrm.credenciales.security;
import bo.edu.uagrm.credenciales.audit.*;
import static bo.edu.uagrm.credenciales.audit.AuditAction.*;
import static bo.edu.uagrm.credenciales.audit.AuditTrail.Resource.*;


import bo.edu.uagrm.credenciales.auth.AuthDtos;
import bo.edu.uagrm.credenciales.rbac.AccessService;
import bo.edu.uagrm.credenciales.user.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class MfaService {
    private final AuditTrail audit;private final UserMfaRepository configs;private final MfaChallengeRepository challenges;private final MfaRecoveryRepository recovery;
    private final AppUserRepository users;private final PasswordEncoder passwords;private final MfaCrypto crypto;
    private final MfaTotp totp;private final SessionRegistry sessions;private final JwtService jwt;private final AccessService access;
    public MfaService(UserMfaRepository configs,MfaChallengeRepository challenges,MfaRecoveryRepository recovery,
                      AppUserRepository users,PasswordEncoder passwords,MfaCrypto crypto,MfaTotp totp,
                      SessionRegistry sessions,JwtService jwt,AccessService access,AuditTrail audit){
        this.audit=audit;this.configs=configs;this.challenges=challenges;this.recovery=recovery;this.users=users;this.passwords=passwords;
        this.crypto=crypto;this.totp=totp;this.sessions=sessions;this.jwt=jwt;this.access=access;
    }
    public record Status(boolean enabled,long recoveryCodesRemaining){}
    public record Setup(String secret,String otpauthUri,Instant expiresAt){@Override public String toString(){return "MfaSetup[redacted]";}}
    public record Enabled(AuthDtos.AuthResponse session,List<String> recoveryCodes){@Override public String toString(){return "MfaEnabled[redacted]";}}
    @Transactional(readOnly=true)
    public boolean required(UUID user){return configs.findById(user).map(c->c.enabled).orElse(false);}
    @Transactional(readOnly=true)
    public Status status(UUID user){return new Status(required(user),recovery.findByUserId(user).stream().filter(c->!c.used).count());}
    public Setup setup(UserPrincipal principal,String password){
        AppUser user=user(principal.userId());requirePassword(user,password);
        var config=configs.locked(user.getId()).orElseGet(()->{var c=new UserMfa();c.userId=user.getId();return c;});
        if(config.enabled)throw new IllegalArgumentException("MFA ya está habilitado.");
        String secret=totp.secret();config.encryptedSecret=crypto.encrypt(user.getId(),secret);config.pendingUntil=Instant.now().plusSeconds(600);config.lastCounter=-1;
        configs.saveAndFlush(config);
        String label="Credenciales:"+principal.email()+(principal.tenantId()==null?"":" ("+principal.tenantId()+")");
        return new Setup(secret,"otpauth://totp/"+URLEncoder.encode(label,StandardCharsets.UTF_8)+"?secret="+secret+"&issuer=Credenciales&algorithm=SHA1&digits=6&period=30",config.pendingUntil);
    }
    public Enabled confirm(UserPrincipal principal,String code){
        var config=configs.locked(principal.userId()).orElseThrow(()->new IllegalArgumentException("Prepara MFA primero."));
        if(config.enabled||config.pendingUntil==null||!config.pendingUntil.isAfter(Instant.now()))throw new IllegalArgumentException("La preparación MFA no está disponible. Solicita otra.");
        long counter=totp.match(crypto.decrypt(config.userId,config.encryptedSecret),code,config.lastCounter);
        if(counter<0)throw new BadCredentialsException("Código inválido o ya utilizado.");
        config.enabled=true;config.generation=UUID.randomUUID();config.pendingUntil=null;config.lastCounter=counter;
        recovery.findByUserId(principal.userId()).forEach(c->c.used=true);
        List<String> codes=new ArrayList<>();
        for(int i=0;i<10;i++){String raw="REC-"+MfaCrypto.randomToken().substring(0,16);var c=new MfaRecoveryCode();c.id=UUID.randomUUID();c.userId=principal.userId();c.codeHash=codeHash(principal.userId(),raw);recovery.save(c);codes.add(raw);}
        sessions.revokeUser(principal.userId());audit.record(MFA_ENABLED,principal.tenantId(),MFA,principal.userId());
        return new Enabled(response(user(principal.userId()),access.principal(user(principal.userId())).withSession(null,true)),List.copyOf(codes));
    }
    public AuthDtos.LoginResult beginLogin(AppUser user,UserPrincipal principal){
        if(!principal.enabled())throw new BadCredentialsException("Credenciales inválidas");
        var config=configs.findById(user.getId()).filter(c->c.enabled).orElse(null);
        if(config==null)return response(user,principal);
        String raw=MfaCrypto.randomToken();var challenge=new MfaChallenge();challenge.id=UUID.randomUUID();challenge.userId=user.getId();
        challenge.generation=config.generation;challenge.tokenHash=MfaCrypto.hash(raw);challenge.expiresAt=Instant.now().plusSeconds(300);challenges.save(challenge);
        return new AuthDtos.MfaChallengeResponse(true,raw,challenge.expiresAt);
    }
    /** Return failure instead of throwing: failed-attempt counters must commit. */
    public Optional<AuthDtos.AuthResponse> verify(String raw,String code){
        var found=challenges.findByTokenHash(MfaCrypto.hash(raw.strip()));
        if(found.isEmpty())return Optional.empty();
        var challenge=found.get();
        if(challenge.consumed||challenge.attempts>=5||!challenge.expiresAt.isAfter(Instant.now()))return Optional.empty();
        challenge.attempts++;
        if(challenge.attempts>=5)challenge.consumed=true;
        var config=configs.locked(challenge.userId).orElse(null);
        var user=users.findById(challenge.userId).orElse(null);
        if(config==null||!config.enabled||!config.generation.equals(challenge.generation)||user==null)return Optional.empty();
        var principal=access.principal(user);
        if(!principal.enabled()||!accept(config,code))return Optional.empty();
        challenge.consumed=true;
        return Optional.of(response(user,principal.withSession(null,true)));
    }
    public boolean disable(UserPrincipal principal,String password,String code){
        requirePassword(user(principal.userId()),password);
        var config=configs.locked(principal.userId()).orElse(null);
        if(config==null||!config.enabled||!accept(config,code))return false;
        config.enabled=false;config.generation=UUID.randomUUID();config.encryptedSecret=null;config.pendingUntil=null;config.lastCounter=-1;
        recovery.findByUserId(principal.userId()).forEach(c->c.used=true);sessions.revokeUser(principal.userId());audit.record(MFA_DISABLED,principal.tenantId(),MFA,principal.userId());return true;
    }
    private boolean accept(UserMfa config,String input){
        String code=input.strip();
        if(code.startsWith("REC-")){
            var found=recovery.findByUserIdAndCodeHashAndUsedFalse(config.userId,codeHash(config.userId,code));
            found.ifPresent(c->c.used=true);return found.isPresent();
        }
        long counter=totp.match(crypto.decrypt(config.userId,config.encryptedSecret),code,config.lastCounter);
        if(counter<0)return false;config.lastCounter=counter;return true;
    }
    private String codeHash(UUID user,String code){return MfaCrypto.hash(user+":"+code);}
    private AppUser user(UUID id){return users.findById(id).filter(AppUser::isActive).orElseThrow(()->new BadCredentialsException("Cuenta no disponible"));}
    private void requirePassword(AppUser user,String password){if(!passwords.matches(password,user.getPasswordHash()))throw new BadCredentialsException("Contraseña actual incorrecta.");}
    private AuthDtos.AuthResponse response(AppUser user,UserPrincipal principal){
        var tenant=user.getTenant();return new AuthDtos.AuthResponse(jwt.create(principal),"Bearer",user.getEmail(),user.getRole().name(),
                tenant==null?null:tenant.getId().toString(),tenant==null?null:tenant.getName(),tenant==null?null:tenant.getInstitutionType());
    }
}
