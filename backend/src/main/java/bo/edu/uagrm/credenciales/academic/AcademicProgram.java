package bo.edu.uagrm.credenciales.academic;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="academic_programs")
public class AcademicProgram {
 @Id UUID id;@Column(name="tenant_id",nullable=false) UUID tenantId;
 @Column(nullable=false,length=180) String name;@Column(nullable=false,length=80) String kind;
 @Column(nullable=false,length=80) String period;@Column(length=2000) String requirements;
 @Column(nullable=false) boolean active=true;@Column(name="created_at",nullable=false) Instant createdAt;@Version long version;
 protected AcademicProgram(){}
}
