package bo.edu.uagrm.credenciales.tenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Set;
public final class OrganizationDtos {
 private OrganizationDtos(){}
 public record Representative(@NotBlank @Size(max=180) String fullName,@NotBlank @Size(max=30) String documentType,
 @NotBlank @Size(max=80) String documentNumber,@NotBlank @Size(max=100) String position,
 @NotBlank @Email @Size(max=180) String email,@NotBlank @Size(max=40) String phone){}
 public record Details(
 @NotBlank @Size(max=160) String legalName,
 @NotBlank @Pattern(regexp="(?i)[a-z]{2}") String country,
 @NotBlank @Size(max=120) String region,
 @NotBlank @Size(max=500) String address,
 @NotBlank @Size(max=40) String phone,
 @NotBlank @Email @Size(max=180) String contactEmail,
 @Size(max=255) @Pattern(regexp="^$|https?://[^\\s]+") String website,
 @Size(max=180) @Pattern(regexp="(?i)^$|[a-z0-9-]+(\\.[a-z0-9-]+)+") String domain,
 @NotBlank @Size(max=80) String taxId,
 @NotBlank @Size(max=120) String educationLicense,
 @Size(max=180) String ministryRegistration,
 @Size(max=500) String accreditation,
 @NotNull InstitutionType institutionType,@NotEmpty @Size(max=5) Set<@NotNull EducationLevel> educationLevels,
 @NotNull @Valid Representative representative){}
 public record Registration(@NotBlank @Size(max=80) String organizationSlug,
 @NotBlank @Size(min=10,max=72) String password,@NotNull @Valid Details profile){}
}
