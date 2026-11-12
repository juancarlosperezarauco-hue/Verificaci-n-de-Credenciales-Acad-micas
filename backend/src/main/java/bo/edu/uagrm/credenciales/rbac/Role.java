package bo.edu.uagrm.credenciales.rbac;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="tenant_roles", uniqueConstraints=@UniqueConstraint(columnNames={"tenant_id","name"}))
public class Role {
 @Id private UUID id;
 @Column(name="tenant_id",nullable=false) private UUID tenantId;
 @Column(nullable=false,length=100) private String name;
 protected Role(){}
 public Role(UUID tenantId,String name){id=UUID.randomUUID();this.tenantId=tenantId;this.name=name;}
 public UUID getId(){return id;}
 public UUID getTenantId(){return tenantId;}
 public String getName(){return name;}
 public void rename(String name){this.name=name;}
}
