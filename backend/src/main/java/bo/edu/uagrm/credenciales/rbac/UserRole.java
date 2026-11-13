package bo.edu.uagrm.credenciales.rbac;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="user_roles",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","role_id"}))
public class UserRole {
 @Id private UUID id;
 @Column(name="tenant_id",nullable=false) private UUID tenantId;
 @Column(name="user_id",nullable=false) private UUID userId;
 @Column(name="role_id",nullable=false) private UUID roleId;
 protected UserRole(){}
 public UserRole(UUID tenantId,UUID userId,UUID roleId){id=UUID.randomUUID();this.tenantId=tenantId;this.userId=userId;this.roleId=roleId;}
 public UUID getRoleId(){return roleId;}
}
