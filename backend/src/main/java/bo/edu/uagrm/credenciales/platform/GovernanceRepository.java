package bo.edu.uagrm.credenciales.platform;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface GovernanceRepository extends JpaRepository<OrganizationGovernance,UUID>{}
