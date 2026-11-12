package bo.edu.uagrm.credenciales.rbac;
import jakarta.persistence.*;
@Entity @Table(name="permissions")
public class Permission {
 @Id @Column(length=60) private String code;
 protected Permission() {}
 public Permission(String code){this.code=code;}
 public String getCode(){return code;}
}
