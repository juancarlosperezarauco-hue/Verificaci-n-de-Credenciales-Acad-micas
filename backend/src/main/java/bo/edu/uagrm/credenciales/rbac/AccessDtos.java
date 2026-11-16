package bo.edu.uagrm.credenciales.rbac;
import jakarta.validation.constraints.*;
import java.util.*;
public final class AccessDtos {
 private AccessDtos(){}
 public record RoleRequest(@NotBlank @Size(max=100) String name,@NotNull @Size(max=19) Set<@NotNull PermissionCode> permissions){}
 public record RoleResponse(UUID id,String name,Set<String> permissions){}
 public record CreateUser(@NotBlank @Size(max=180) String fullName,@NotBlank @Email @Size(max=180) String email,
  @NotBlank @Size(min=10,max=72) String password,@NotNull @Size(max=50) Set<@NotNull UUID> roleIds){}
 public record UpdateUser(@NotBlank @Size(max=180) String fullName,@NotNull @Size(max=50) Set<@NotNull UUID> roleIds){}
 public record UserResponse(UUID id,String fullName,String email,boolean active,String role,Set<UUID> roleIds){}
}
