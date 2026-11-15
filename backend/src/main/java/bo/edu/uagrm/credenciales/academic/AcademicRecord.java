package bo.edu.uagrm.credenciales.academic;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity @Table(name="academic_records")
public class AcademicRecord {
 public enum Status {ENROLLED,COMPLETED,WITHDRAWN}
 @Id UUID id;@Column(name="tenant_id",nullable=false) UUID tenantId;@Column(name="student_id",nullable=false) UUID studentId;
 @Column(name="program_id",nullable=false) UUID programId;@Column(name="registration_code",nullable=false,length=80) String registrationCode;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) Status status;
 @Column(name="enrolled_on",nullable=false) LocalDate enrolledOn;@Column(name="completed_on") LocalDate completedOn;
 @Column(length=120) String result;@Column(length=2000) String notes;
 @Column(name="created_at",nullable=false) Instant createdAt;@Version long version;
 protected AcademicRecord(){}
 public UUID getId(){return id;}public UUID getTenantId(){return tenantId;}public UUID getStudentId(){return studentId;}public UUID getProgramId(){return programId;}
}
