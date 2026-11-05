package bo.edu.uagrm.credenciales.student;

import bo.edu.uagrm.credenciales.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "students")
public class Student {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "full_name", nullable = false, length = 180)
    private String fullName;

    @Column(name = "document_number", nullable = false, length = 80)
    private String documentNumber;

    @Column(length = 180)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(length = 220)
    private String program;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Student() {}

    public Student(Tenant tenant, String fullName, String documentNumber, String email, String phone,
                   String program) {
        this.id = UUID.randomUUID();
        this.tenant = tenant;
        this.fullName = fullName;
        this.documentNumber = documentNumber;
        this.email = email;
        this.phone = phone;
        this.program = program;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public String getFullName() { return fullName; }
    public String getDocumentNumber() { return documentNumber; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getProgram() { return program; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
