package bo.edu.uagrm.credenciales.identity;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;
public final class IdentityDtos {
    private IdentityDtos() {}
    public record Register(
        @Size(max=180) String fullName,
        @Pattern(regexp="[A-Za-z]{2}") String country,
        @Pattern(regexp="CI|DNI|PASAPORTE|OTRO") String documentType,
        @Size(max=80) String documentNumber,
        @Past LocalDate dateOfBirth,
        @Size(max=120) String department,
        @NotBlank @Email @Size(max=180) String email,
        @NotBlank @Size(min=10,max=72) String password) {}
    public record Login(@NotBlank @Email String email, @NotBlank String password) {}
    public record UpdateProfile(
        @NotBlank @Size(max=180) String fullName,
        @NotBlank @Pattern(regexp="[A-Za-z]{2}") String country,
        @NotBlank @Pattern(regexp="CI|DNI|PASAPORTE|OTRO") String documentType,
        @NotBlank @Size(max=80) String documentNumber,
        @NotNull @Past LocalDate dateOfBirth,
        @NotBlank @Size(max=120) String department) {}
    public record OwnProfile(UUID id, String fullName, String country, String documentType,
                             String documentNumber, LocalDate dateOfBirth, String department,
                             String searchCode, boolean profileComplete) {
        public static OwnProfile from(StudentAccount s) {
            return new OwnProfile(s.getId(), s.getFullName(), s.getCountry(), s.getDocumentType(),
                    s.getDocumentNumber(), s.getDateOfBirth(), s.getDepartment(), s.getSearchCode(),
                    s.isProfileComplete());
        }
    }
    public record Profile(UUID id, String fullName, String country, String documentType, String documentNumber,
                          LocalDate dateOfBirth, String department) {
        public static Profile from(StudentAccount s) {
            return new Profile(s.getId(), s.getFullName(), s.getCountry(), s.getDocumentType(),
                    s.getDocumentNumber(), s.getDateOfBirth(), s.getDepartment());
        }
    }
}
