package bo.edu.uagrm.credenciales.blockchain;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DevelopmentBlockchainAdapter implements BlockchainAnchorPort {
    @Override
    public String anchor(UUID tenantId, UUID credentialId, String documentHash) {
        return "pending:" + credentialId;
    }
}

