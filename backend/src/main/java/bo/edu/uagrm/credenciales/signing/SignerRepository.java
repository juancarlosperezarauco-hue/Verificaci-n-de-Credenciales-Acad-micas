package bo.edu.uagrm.credenciales.signing;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SignerRepository extends JpaRepository<InstitutionalSigner,UUID>{}
