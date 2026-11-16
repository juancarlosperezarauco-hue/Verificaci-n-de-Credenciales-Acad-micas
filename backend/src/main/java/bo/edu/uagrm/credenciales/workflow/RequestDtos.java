package bo.edu.uagrm.credenciales.workflow;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.*;

public final class RequestDtos {
    public record Create(
            @NotNull UUID recordId,
            @NotBlank @Pattern(regexp = "DIPLOMA|CERTIFICADO|TITULO|RECONOCIMIENTO|CONSTANCIA")
                    String credentialType,
            @NotBlank @Size(max = 220) String title,
            @Size(max = 2000) String message,
            UUID templateVersionId) {}

    public record Correction(
            @NotNull UUID recordId,
            @NotBlank @Size(max = 220) String title,
            @NotBlank @Size(min = 10, max = 2000) String reason,
            UUID templateVersionId) {}

    public record Action(
            @NotBlank @Pattern(regexp = "START|OBSERVE|RECOMMEND|APPROVE|REJECT|RESUBMIT|WITHDRAW")
                    String action,
            @Size(max = 2000) String reason) {}

    public record Update(
            @NotBlank @Size(max = 220) String title,
            @Size(max = 2000) String message,
            UUID templateVersionId) {}

    public record Attachment(UUID id, String name, String contentType, Instant createdAt) {}

    public record Event(UUID id, String action, String status, String reason, Instant occurredAt) {}

    public record View(
            UUID id,
            UUID tenantId,
            String institutionName,
            UUID studentId,
            String studentName,
            UUID recordId,
            String programName,
            String credentialType,
            String title,
            String message,
            CredentialRequest.Status status,
            UUID reviewerId,
            UUID approverId,
            UUID templateVersionId,
            UUID sourceCredentialId,
            UUID credentialId,
            boolean profileChanged,
            Instant createdAt,
            Instant updatedAt,
            List<Attachment> attachments,
            List<Event> events) {}
}
