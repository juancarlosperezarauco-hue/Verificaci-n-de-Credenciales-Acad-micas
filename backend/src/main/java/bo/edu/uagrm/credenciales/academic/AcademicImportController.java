package bo.edu.uagrm.credenciales.academic;

import bo.edu.uagrm.credenciales.security.UserPrincipal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/institution/academic/imports")
@PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
public class AcademicImportController {
    private final AcademicImportService service;

    public AcademicImportController(AcademicImportService service) {
        this.service = service;
    }

    public record Input(
            @NotNull @Size(min = 1, max = 100) List<AcademicDtos.@NotNull @Valid Enroll> rows) {}

    @PostMapping("/preview")
    public AcademicImportService.Preview preview(
            @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody Input input) {
        return service.prepare(p, input.rows());
    }

    @PostMapping("/{id}/apply")
    public AcademicImportService.Applied apply(
            @AuthenticationPrincipal UserPrincipal p, @PathVariable UUID id) {
        return service.apply(p, id);
    }
}
