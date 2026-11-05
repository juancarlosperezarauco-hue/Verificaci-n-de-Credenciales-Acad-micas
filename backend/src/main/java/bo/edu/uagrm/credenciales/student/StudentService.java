package bo.edu.uagrm.credenciales.student;

import bo.edu.uagrm.credenciales.tenant.Tenant;
import bo.edu.uagrm.credenciales.tenant.TenantRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {
    private final StudentRepository studentRepository;
    private final TenantRepository tenantRepository;

    public StudentService(StudentRepository studentRepository, TenantRepository tenantRepository) {
        this.studentRepository = studentRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentDtos.Response> list(UUID tenantId) {
        return studentRepository.findAllByTenantIdOrderByFullNameAsc(tenantId).stream()
                .map(StudentDtos.Response::from).toList();
    }

    @Transactional
    public StudentDtos.Response create(UUID tenantId, StudentDtos.CreateRequest request) {
        String documentNumber = request.documentNumber().trim();
        if (studentRepository.existsByTenantIdAndDocumentNumberIgnoreCase(tenantId, documentNumber)) {
            throw new IllegalArgumentException("Ya existe un estudiante con ese documento");
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));
        Student student = new Student(tenant, request.fullName().trim(), documentNumber,
                clean(request.email()), clean(request.phone()), clean(request.program()));
        return StudentDtos.Response.from(studentRepository.save(student));
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
