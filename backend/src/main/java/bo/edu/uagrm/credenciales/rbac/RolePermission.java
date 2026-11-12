package bo.edu.uagrm.credenciales.rbac;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="role_permissions", uniqueConstraints=@UniqueConstraint(columnNames={"role_id","permission_code"}))
public class RolePermission {
 @Id private UUID id;
 @Column(name="role_id",nullable=false) private UUID roleId;
 @Column(name="permission_code",nullable=false,length=60) private String permissionCode;
 protected RolePermission(){}
 public RolePermission(UUID roleId,String permissionCode){id=UUID.randomUUID();this.roleId=roleId;this.permissionCode=permissionCode;}
 public String getPermissionCode(){return permissionCode;}
}
