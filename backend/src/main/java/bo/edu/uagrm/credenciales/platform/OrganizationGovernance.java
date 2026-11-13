package bo.edu.uagrm.credenciales.platform;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="organization_governance")
public class OrganizationGovernance {
 @Id @Column(name="tenant_id") private UUID tenantId;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private OrganizationStatus status;
 @Version private long version;
 protected OrganizationGovernance(){}
 public OrganizationGovernance(UUID id,boolean active){tenantId=id;status=active?OrganizationStatus.ACTIVE:OrganizationStatus.INACTIVE;}
 public OrganizationGovernance(UUID id,OrganizationStatus status){tenantId=id;this.status=status;}
 public OrganizationStatus getStatus(){return status;}
 public void change(OrganizationStatus status){this.status=status;}
}
