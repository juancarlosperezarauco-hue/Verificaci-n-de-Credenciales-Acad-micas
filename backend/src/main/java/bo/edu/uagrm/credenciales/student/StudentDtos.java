package bo.edu.uagrm.credenciales.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class StudentDtos {
    private StudentDtos() {}

    public record CreateRequest(
            @NotBlank @Size(max = 180) String fullName,
            @NotBlank @Size(max = 80) String documentNumber,
            @Email @Size(max = 180) String email,
            @Size(max = 40) String phone,
            @Size(max = 220) String program) {}

    public record Response(UUID id, String fullName, String documentNumber, String email, String phone,
                           String program, boolean active, Instant createdAt) {
        static Response from(Student student) {
            return new Response(student.getId(), student.getFullName(), student.getDocumentNumber(),
                    student.getEmail(), student.getPhone(), student.getProgram(), student.isActive(),
                    student.getCreatedAt());
        }
    }
}
