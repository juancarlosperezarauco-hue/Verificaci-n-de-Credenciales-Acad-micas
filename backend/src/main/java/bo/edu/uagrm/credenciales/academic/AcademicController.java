package bo.edu.uagrm.credenciales.academic;

import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.shared.ResultPage;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class AcademicController {
    private final AcademicService service;

    public AcademicController(AcademicService s) {
        service = s;
    }

    @GetMapping("/api/institution/academic/programs")
    @PreAuthorize("hasAuthority('ACADEMIC_VIEW')")
    List<AcademicDtos.Program> programs(@AuthenticationPrincipal UserPrincipal p) {
        return service.programs(p);
    }

    @PostMapping("/api/institution/academic/programs")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    AcademicDtos.Program create(
            @AuthenticationPrincipal UserPrincipal p,
            @Valid @RequestBody AcademicDtos.ProgramInput r) {
        return service.saveProgram(p, null, r);
    }

    @PutMapping("/api/institution/academic/programs/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    AcademicDtos.Program update(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable UUID id,
            @Valid @RequestBody AcademicDtos.ProgramInput r) {
        return service.saveProgram(p, id, r);
    }

    @GetMapping("/api/institution/academic/records")
    @PreAuthorize("hasAuthority('ACADEMIC_VIEW')")
    ResultPage<AcademicDtos.Record> records(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(p, studentId, page, size);
    }

    @PostMapping("/api/institution/academic/records")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    AcademicDtos.Record enroll(
            @AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody AcademicDtos.Enroll r) {
        return service.enroll(p, r);
    }

    @PatchMapping("/api/institution/academic/records/{id}")
    @PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
    AcademicDtos.Record update(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable UUID id,
            @Valid @RequestBody AcademicDtos.Update r) {
        return service.update(p, id, r);
    }

    @GetMapping("/api/me/academic-records")
    @PreAuthorize("hasRole('STUDENT')")
    List<AcademicDtos.Record> own(@AuthenticationPrincipal UserPrincipal p) {
        return service.own(p.userId());
    }
}
