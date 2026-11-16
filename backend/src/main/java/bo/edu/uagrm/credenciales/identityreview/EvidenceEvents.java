package bo.edu.uagrm.credenciales.identityreview;

import java.util.UUID;

public final class EvidenceEvents {
    private EvidenceEvents() {}

    public record ProfileChanged(UUID student, boolean deactivated) {}

    public record ConsentWithdrawn(UUID consent) {}
}
