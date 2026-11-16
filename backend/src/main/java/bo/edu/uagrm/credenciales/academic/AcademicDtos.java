package bo.edu.uagrm.credenciales.academic;

import jakarta.validation.constraints.*;

import java.time.*;
import java.util.UUID;

public final class AcademicDtos {
    public record ProgramInput(
            @NotBlank @Size(max = 180) String name,
            @NotBlank @Size(max = 80) String kind,
            @NotBlank @Size(max = 80) String period,
            @Size(max = 2000) String requirements,
            boolean active) {}

    public record Program(
            UUID id,
            String name,
            String kind,
            String period,
            String requirements,
            boolean active) {}

    public record Enroll(
            @NotNull UUID studentId,
            @NotNull UUID programId,
            @NotBlank @Size(max = 80) String registrationCode,
            @NotNull LocalDate enrolledOn) {}

    public record Update(
            @NotNull AcademicRecord.Status status,
            LocalDate completedOn,
            @Size(max = 120) String result,
            @Size(max = 2000) String notes) {}

    public record Record(
            UUID id,
            UUID tenantId,
            String institutionName,
            UUID studentId,
            String studentName,
            UUID programId,
            String programName,
            String period,
            String requirements,
            String registrationCode,
            AcademicRecord.Status status,
            LocalDate enrolledOn,
            LocalDate completedOn,
            String result,
            String notes,
            Instant createdAt) {}
}
