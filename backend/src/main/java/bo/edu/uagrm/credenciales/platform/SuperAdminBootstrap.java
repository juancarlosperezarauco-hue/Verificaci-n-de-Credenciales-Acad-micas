package bo.edu.uagrm.credenciales.platform;
import bo.edu.uagrm.credenciales.user.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
@Component
public class SuperAdminBootstrap implements ApplicationRunner {
 private final AppUserRepository users;private final PasswordEncoder encoder;private final String email;private final String password;
 public SuperAdminBootstrap(AppUserRepository users,PasswordEncoder encoder,
 @Value("${BOOTSTRAP_SUPERADMIN_EMAIL:}") String email,@Value("${BOOTSTRAP_SUPERADMIN_PASSWORD:}") String password){this.users=users;this.encoder=encoder;this.email=email;this.password=password;}
 @Override @Transactional public void run(ApplicationArguments args){
  if(email.isBlank()&&password.isBlank())return;
  if(!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")||password.length()<12||password.length()>72)throw new IllegalStateException("Configuración de bootstrap superadmin inválida");
  String normalized=email.trim().toLowerCase(java.util.Locale.ROOT);
  if(users.findByTenantIsNullAndEmail(normalized).isEmpty())users.save(new AppUser(null,normalized,encoder.encode(password),UserRole.SUPER_ADMIN));
 }
}
