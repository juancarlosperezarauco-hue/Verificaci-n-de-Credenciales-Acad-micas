package bo.edu.uagrm.credenciales.identity;
import bo.edu.uagrm.credenciales.auth.AuthDtos;
import bo.edu.uagrm.credenciales.security.*;
import bo.edu.uagrm.credenciales.user.UserRole;
import java.util.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {
    private final StudentAccountRepository accounts;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    public IdentityService(StudentAccountRepository accounts, PasswordEncoder passwords, JwtService jwt) {
        this.accounts=accounts; this.passwords=passwords; this.jwt=jwt;
    }
    @Transactional
    public AuthDtos.AuthResponse register(IdentityDtos.Register r) {
        String country=r.country().toUpperCase(Locale.ROOT);
        String number=r.documentNumber().strip().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        String email=r.email().strip().toLowerCase(Locale.ROOT);
        if(number.isBlank()) throw new IllegalArgumentException("Documento inválido");
        if(accounts.findByEmail(email).isPresent() ||
           accounts.existsByCountryAndDocumentTypeAndDocumentNumber(country,r.documentType(),number))
            throw new IllegalArgumentException("Ya existe una cuenta con esos datos. Inicia sesión o solicita revisión.");
        return session(accounts.saveAndFlush(new StudentAccount(r.fullName().strip(), country, r.documentType(),
                number, email, passwords.encode(r.password()))));
    }
    @Transactional(readOnly=true)
    public AuthDtos.AuthResponse login(IdentityDtos.Login r) {
        StudentAccount s=accounts.findByEmail(r.email().strip().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        if(!passwords.matches(r.password(),s.getPasswordHash())) throw new BadCredentialsException("Credenciales inválidas");
        return session(s);
    }
    @Transactional(readOnly=true)
    public IdentityDtos.OwnProfile profile(UUID id) { return IdentityDtos.OwnProfile.from(require(id)); }
    @Transactional
    public IdentityDtos.OwnProfile rotateSearchCode(UUID id) {
        StudentAccount student = accounts.findLockedById(id)
            .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        student.rotateSearchCode();
        return IdentityDtos.OwnProfile.from(student);
    }
    @Transactional
    public StudentAccount findBySearchCode(String code) {
        return accounts.findBySearchCode(code.strip().toUpperCase(Locale.ROOT))
            .orElseThrow(() -> new IllegalArgumentException("Código de estudiante no encontrado."));
    }
    public StudentAccount require(UUID id) {
        return accounts.findById(id).orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
    }
    private AuthDtos.AuthResponse session(StudentAccount s) {
        return new AuthDtos.AuthResponse(jwt.create(principal(s)), "Bearer", s.getEmail(), "STUDENT", null, null, null);
    }
    public static UserPrincipal principal(StudentAccount s) {
        return new UserPrincipal(s.getId(), null, s.getEmail(), s.getPasswordHash(), UserRole.STUDENT, true);
    }
}
