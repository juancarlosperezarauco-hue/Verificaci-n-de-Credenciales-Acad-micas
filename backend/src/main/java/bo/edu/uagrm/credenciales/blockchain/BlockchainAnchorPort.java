package bo.edu.uagrm.credenciales.blockchain;

import java.util.UUID;

public interface BlockchainAnchorPort {
    String anchor(UUID tenantId, UUID credentialId, String documentHash);
}

