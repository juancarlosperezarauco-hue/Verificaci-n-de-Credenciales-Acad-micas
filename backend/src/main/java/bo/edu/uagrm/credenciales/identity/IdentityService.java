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
        String email=r.email().strip().toLowerCase(Locale.ROOT);
        if(accounts.findByEmail(email).isPresent())
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo. Inicia sesión o recupera tu acceso.");
        boolean anyProfileField = hasText(r.fullName()) || hasText(r.country()) || hasText(r.documentType())
                || hasText(r.documentNumber()) || r.dateOfBirth() != null || hasText(r.department());
        boolean completeProfile = hasText(r.fullName()) && hasText(r.country()) && hasText(r.documentType())
                && hasText(r.documentNumber()) && r.dateOfBirth() != null && hasText(r.department());
        if (anyProfileField && !completeProfile)
            throw new IllegalArgumentException("Completa todos los datos del perfil o regístrate solo con correo y contraseña.");
        StudentAccount account;
        if (completeProfile) {
            String country = normalizeCountry(r.country());
            String number = normalizeDocument(r.documentNumber());
            ensureIdentityAvailable(null, country, r.documentType(), number);
            account = new StudentAccount(r.fullName().strip(), country, r.documentType(), number,
                    r.dateOfBirth(), r.department().strip(), email, passwords.encode(r.password()));
        } else {
            account = new StudentAccount(email, passwords.encode(r.password()));
        }
        return session(accounts.saveAndFlush(account));
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
    public IdentityDtos.OwnProfile updateProfile(UUID id, IdentityDtos.UpdateProfile request) {
        StudentAccount student = accounts.findLockedById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        String country = normalizeCountry(request.country());
        String number = normalizeDocument(request.documentNumber());
        ensureIdentityAvailable(id, country, request.documentType(), number);
        student.updateProfile(request.fullName().strip(), country, request.documentType(), number,
                request.dateOfBirth(), request.department().strip());
        return IdentityDtos.OwnProfile.from(student);
    }
    @Transactional
    public IdentityDtos.OwnProfile rotateSearchCode(UUID id) {
        StudentAccount student = accounts.findLockedById(id)
            .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        ensureProfileComplete(student);
        student.rotateSearchCode();
        return IdentityDtos.OwnProfile.from(student);
    }
    @Transactional
    public StudentAccount findBySearchCode(String code) {
        StudentAccount student = accounts.findBySearchCode(code.strip().toUpperCase(Locale.ROOT))
            .orElseThrow(() -> new IllegalArgumentException("Código de estudiante no encontrado."));
        ensureProfileComplete(student);
        return student;
    }
    public StudentAccount require(UUID id) {
        return accounts.findById(id).orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
    }
    public StudentAccount requireComplete(UUID id) {
        StudentAccount student = require(id);
        ensureProfileComplete(student);
        return student;
    }
    private void ensureIdentityAvailable(UUID currentId, String country, String type, String number) {
        accounts.findByCountryAndDocumentTypeAndDocumentNumber(country, type, number).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId))
                throw new IllegalArgumentException("Ya existe una cuenta con ese documento. Solicita revisión si te pertenece.");
        });
    }
    private void ensureProfileComplete(StudentAccount student) {
        if (!student.isProfileComplete())
            throw new IllegalArgumentException("El estudiante todavía debe completar su perfil personal.");
    }
    private String normalizeCountry(String value) { return value.strip().toUpperCase(Locale.ROOT); }
    private String normalizeDocument(String value) {
        String normalized = value.strip().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        if (normalized.isBlank()) throw new IllegalArgumentException("Documento inválido");
        return normalized;
    }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private AuthDtos.AuthResponse session(StudentAccount s) {
        return new AuthDtos.AuthResponse(jwt.create(principal(s)), "Bearer", s.getEmail(), "STUDENT", null, null, null);
    }
    public static UserPrincipal principal(StudentAccount s) {
        return new UserPrincipal(s.getId(), null, s.getEmail(), s.getPasswordHash(), UserRole.STUDENT, true);
    }
}
