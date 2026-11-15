package bo.edu.uagrm.credenciales.identityreview;

import bo.edu.uagrm.credenciales.identity.StudentAccount;

import jakarta.validation.constraints.*;

import java.time.*;
import java.util.*;

public final class IdentityReviewDtos {
    private IdentityReviewDtos() {}

    public record Snapshot(
            String fullName,
            String country,
            String documentType,
            String documentNumber,
            LocalDate dateOfBirth,
            String department) {
        static Snapshot from(StudentAccount a) {
            return new Snapshot(
                    a.getFullName(),
                    a.getCountry(),
                    a.getDocumentType(),
                    a.getDocumentNumber(),
                    a.getDateOfBirth(),
                    a.getDepartment());
        }

        @Override
        public String toString() {
            return "IdentitySnapshot[private]";
        }
    }

    public record Target(UUID id, String name, String slug) {}

    public record Item(
            UUID id,
            UUID studentId,
            UUID tenantId,
            String institutionName,
            String status,
            String effectiveStatus,
            Snapshot profile,
            boolean profileChanged,
            boolean sharingActive,
            boolean photoPresent,
            Instant submittedAt,
            Instant reviewedAt,
            Instant withdrawnAt,
            String reason,
            boolean documentsAvailable) {}

    public record Own(
            boolean profileComplete,
            boolean emailVerified,
            List<Target> institutions,
            List<Item> submissions) {}

    public record Results(List<Item> items, long total, int page, int size) {}

    public record Decision(
            @NotNull IdentitySubmission.Status status, @Size(max = 1000) String reason) {}
}
