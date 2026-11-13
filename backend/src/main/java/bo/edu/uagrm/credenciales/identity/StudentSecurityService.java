package bo.edu.uagrm.credenciales.identity;

import bo.edu.uagrm.credenciales.consent.ConsentRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class StudentSecurityService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final StudentAccountRepository accounts;
    private final StudentAccountTokenRepository tokens;
    private final PasswordEncoder passwords;
    private final ApplicationEventPublisher events;
    private final ConsentRepository consents;
    private final String frontendUrl;

    public StudentSecurityService(StudentAccountRepository accounts, StudentAccountTokenRepository tokens,
                                  PasswordEncoder passwords, ApplicationEventPublisher events, ConsentRepository consents,
                                  @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.accounts=accounts; this.tokens=tokens; this.passwords=passwords;
        this.events=events; this.consents=consents; this.frontendUrl=frontendUrl.replaceAll("/+$", "");
    }

    @Transactional(readOnly=true)
    public StudentSecurityDtos.Snapshot snapshot(UUID id) {
        return StudentSecurityDtos.Snapshot.from(accounts.findById(id).filter(StudentAccount::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no disponible.")));
    }

    public void onRegistered(UUID id) { sendVerification(lockAccount(id)); }

    public void resendVerification(UUID id) {
        StudentAccount account=lockAccount(id);
        if (!account.isEmailVerified()) sendVerification(account);
    }

    public void verifyEmail(String raw) {
        var token=consume(raw, StudentTokenType.EMAIL_VERIFICATION);
        if (!token.getStudent().getEmail().equals(token.getPendingEmail())) throw invalidToken();
        token.getStudent().verifyEmail();
    }

    public void requestPasswordReset(String email) {
        String normalized=normalizeEmail(email);
        accounts.findIdByEmail(normalized).ifPresent(id -> {
            StudentAccount account=accounts.findLockedById(id).orElseThrow();
            if (!account.isActive() || !account.getEmail().equals(normalized)) return;
            String raw=issue(account, StudentTokenType.PASSWORD_RESET, null, Duration.ofMinutes(30));
            mail(account.getEmail(), "Recupera tu contraseña", "/student/recover?token="+raw);
        });
    }

    public void resetPassword(String raw, String newPassword) {
        validatePassword(newPassword);
        var token=consume(raw, StudentTokenType.PASSWORD_RESET);
        token.getStudent().changePasswordHash(passwords.encode(newPassword));
        invalidateAll(token.getStudent().getId());
    }

    public void changePassword(UUID id, String currentPassword, String newPassword) {
        validatePassword(newPassword);
        StudentAccount account=lockAccount(id);
        requirePassword(account, currentPassword);
        account.changePasswordHash(passwords.encode(newPassword));
        invalidateAll(id);
    }

    public void requestEmailChange(UUID id, String currentPassword, String newEmail) {
        StudentAccount account=lockAccount(id);
        requirePassword(account, currentPassword);
        String email=normalizeEmail(newEmail);
        if (email.equals(account.getEmail())) throw new IllegalArgumentException("Ese ya es tu correo actual.");
        if (accounts.findByEmail(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo no está disponible.");
        String raw=issue(account, StudentTokenType.EMAIL_CHANGE, email, Duration.ofMinutes(30));
        mail(email, "Confirma tu nuevo correo", "/student/confirm-email?type=change&token="+raw);
    }

    public void confirmEmailChange(String raw) {
        var token=consume(raw, StudentTokenType.EMAIL_CHANGE);
        if (accounts.findByEmail(token.getPendingEmail()).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo no está disponible.");
        token.getStudent().changeEmail(token.getPendingEmail());
        invalidateAll(token.getStudent().getId());
    }

    public void deactivate(UUID id, String currentPassword) {
        StudentAccount account=lockAccount(id);
        requirePassword(account, currentPassword);
        account.deactivate();
        invalidateAll(id);
        consents.revokeAllByStudent(id, Instant.now());
    }

    private StudentAccount lockAccount(UUID id) {
        return accounts.findLockedById(id).filter(StudentAccount::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no disponible."));
    }

    private StudentAccountToken consume(String raw, StudentTokenType type) {
        String hash=hash(raw.strip());
        // All issuers/consumers lock the account first, then the token: no lock-order inversion.
        var found=tokens.findByTokenHashAndTokenType(hash, type).orElseThrow(this::invalidToken);
        lockAccount(found.getStudent().getId());
        var token=tokens.findByTokenHashAndTokenTypeAndUsedAtIsNull(hash, type).orElseThrow(this::invalidToken);
        if (!token.getExpiresAt().isAfter(Instant.now())) throw invalidToken();
        token.use();
        return token;
    }

    private String issue(StudentAccount account, StudentTokenType type, String email, Duration duration) {
        tokens.invalidate(account.getId(), type, Instant.now());
        byte[] bytes=new byte[32]; RANDOM.nextBytes(bytes);
        String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new StudentAccountToken(account, hash(raw), type, email, Instant.now().plus(duration)));
        return raw;
    }

    private void invalidateAll(UUID id) {
        for (var type:StudentTokenType.values()) tokens.invalidate(id, type, Instant.now());
    }

    private void sendVerification(StudentAccount account) {
        String raw=issue(account, StudentTokenType.EMAIL_VERIFICATION, account.getEmail(), Duration.ofHours(24));
        mail(account.getEmail(), "Verifica tu correo", "/student/confirm-email?type=verification&token="+raw);
    }

    private void mail(String to, String subject, String path) {
        events.publishEvent(new StudentMailDelivery.Message(to, subject, "Abre este enlace para continuar:\n"+frontendUrl+path));
    }

    private void validatePassword(String password) {
        if (password.length()<10 || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new IllegalArgumentException("Usa entre 10 y 72 caracteres y un máximo de 72 bytes UTF-8.");
    }

    private void requirePassword(StudentAccount account, String value) {
        if (!passwords.matches(value, account.getPasswordHash())) throw new BadCredentialsException("Contraseña actual incorrecta.");
    }

    private IllegalArgumentException invalidToken() { return new IllegalArgumentException("El enlace no es válido, ha vencido o ya fue utilizado."); }
    private String normalizeEmail(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    private String hash(String raw) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 no disponible", exception); }
    }
}
