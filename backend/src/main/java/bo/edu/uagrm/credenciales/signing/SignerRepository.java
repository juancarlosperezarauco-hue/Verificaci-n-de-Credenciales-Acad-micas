package bo.edu.uagrm.credenciales.signing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SignerRepository extends JpaRepository<InstitutionalSigner, UUID> {}
