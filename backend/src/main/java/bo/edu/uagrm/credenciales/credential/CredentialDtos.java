package bo.edu.uagrm.credenciales.credential;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class CredentialDtos {
    private CredentialDtos() {}

    public record CreateRequest(
            @NotBlank @Size(max = 180) String holderName,
            @Size(max = 80) String holderDocument,
            @NotBlank @Size(max = 60) String credentialType,
            @NotBlank @Size(max = 220) String title) {}

    public record RevokeRequest(@NotBlank @Size(max = 500) String reason) {}

    public record Response(UUID id, String holderName, String credentialType, String title,
                           CredentialStatus status, String verificationCode, String documentName, String documentHash,
                           String blockchainTxId, Instant issuedAt, Instant createdAt) {
        static Response from(Credential c) {
            return new Response(c.getId(), c.getHolderName(), c.getCredentialType(), c.getTitle(),
                    c.getStatus(), c.getVerificationCode(), c.getDocumentName(), c.getDocumentHash(), c.getBlockchainTxId(),
                    c.getIssuedAt(), c.getCreatedAt());
        }
    }

    public record PublicVerification(String organization, String holderName, String credentialType,
                                     String title, CredentialStatus status, String verificationCode,
                                     String documentHash, Instant issuedAt, Instant revokedAt,
                                     String revocationReason) {
        static PublicVerification from(Credential c) {
            return new PublicVerification(c.getTenant().getName(), c.getHolderName(), c.getCredentialType(),
                    c.getTitle(), c.getStatus(), c.getVerificationCode(), c.getDocumentHash(),
                    c.getIssuedAt(), c.getRevokedAt(), c.getRevocationReason());
        }
    }
}

