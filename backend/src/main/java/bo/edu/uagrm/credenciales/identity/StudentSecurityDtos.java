package bo.edu.uagrm.credenciales.identity;

import jakarta.validation.constraints.*;

public final class StudentSecurityDtos {
    private StudentSecurityDtos() {}
    public record TokenRequest(@NotBlank @Size(max=128) String token) {}
    public record PasswordResetRequest(@NotBlank @Email @Size(max=180) String email) {}
    public record PasswordResetConfirm(@NotBlank @Size(max=128) String token,
            @NotBlank @Size(min=10,max=72) String newPassword) {}
    public record PasswordChange(@NotBlank @Size(max=128) String currentPassword,
            @NotBlank @Size(min=10,max=72) String newPassword) {}
    public record EmailChange(@NotBlank @Size(max=128) String currentPassword,
            @NotBlank @Email @Size(max=180) String newEmail) {}
    public record Deactivate(@NotBlank @Size(max=128) String currentPassword) {}
    public record Snapshot(String email, boolean emailVerified, boolean active) {
        static Snapshot from(StudentAccount account) {
            return new Snapshot(account.getEmail(), account.isEmailVerified(), account.isActive());
        }
    }
}
