package bo.edu.uagrm.credenciales.auth;

import bo.edu.uagrm.credenciales.tenant.InstitutionType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 160) String organizationName,
            @NotBlank @Size(max = 80) String organizationSlug,
            @NotNull InstitutionType institutionType,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 10, max = 72) String password) {}

    public record LoginRequest(
            @NotBlank String organizationSlug,
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record AuthResponse(String token, String tokenType, String email, String role, String tenantId,
                               String institutionName, InstitutionType institutionType) {}
}

