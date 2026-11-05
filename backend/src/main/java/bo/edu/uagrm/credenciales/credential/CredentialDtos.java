package bo.edu.uagrm.credenciales.credential;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CredentialDtos {
    private CredentialDtos() {}

    public record CreateRequest(
            @NotNull UUID studentId,
            @NotBlank @Pattern(regexp = "DIPLOMA|CERTIFICADO|TITULO") String credentialType,
            @NotBlank @Size(max = 220) String title) {}

    public record CreateBatchRequest(
            @NotNull @Size(min = 1, max = 100) List<@NotNull UUID> studentIds,
            @NotBlank @Pattern(regexp = "DIPLOMA|CERTIFICADO|TITULO|RECONOCIMIENTO|CONSTANCIA") String credentialType,
            @NotBlank @Size(max = 220) String title) {}

    public record RevokeRequest(@NotBlank @Size(max = 500) String reason) {}

    public record Response(UUID id, UUID studentId, String holderName, String credentialType, String title,
                           CredentialStatus status, String verificationCode, String documentName, String documentHash,
                           String blockchainTxId, Instant issuedAt, Instant createdAt) {
        static Response from(Credential c) {
            return new Response(c.getId(), c.getStudentAccount() != null ? c.getStudentAccount().getId()
                    : c.getStudent() == null ? null : c.getStudent().getId(),
                    c.getHolderName(), c.getCredentialType(), c.getTitle(),
                    c.getStatus(), c.getVerificationCode(), c.getDocumentName(), c.getDocumentHash(), c.getBlockchainTxId(),
                    c.getIssuedAt(), c.getCreatedAt());
        }
    }

    public record PublicVerification(String organization, String institutionType, String institutionLogoUrl,
                                     String primaryColor, String secondaryColor, String holderName, String credentialType,
                                     String title, CredentialStatus status, String verificationCode,
                                     String documentHash, Instant issuedAt, Instant revokedAt,
                                     String revocationReason) {
        static PublicVerification from(Credential c) {
            String logoUrl = c.getTenant().getLogoObjectKey() == null ? null
                    : "/api/public/institutions/" + c.getTenant().getId() + "/logo?v="
                    + Integer.toUnsignedString(c.getTenant().getLogoObjectKey().hashCode());
            return new PublicVerification(c.getTenant().getName(), c.getTenant().getInstitutionType().name(),
                    logoUrl, c.getTenant().getPrimaryColor(), c.getTenant().getSecondaryColor(),
                    c.getHolderName(), c.getCredentialType(),
                    c.getTitle(), c.getStatus(), c.getVerificationCode(), c.getDocumentHash(),
                    c.getIssuedAt(), c.getRevokedAt(), c.getRevocationReason());
        }
    }

    public record StoredDocument(byte[] content, String contentType, String name) {}
}

