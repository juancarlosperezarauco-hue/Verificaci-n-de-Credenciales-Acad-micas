package bo.edu.uagrm.credenciales;

import static org.junit.jupiter.api.Assertions.assertThrows;
import bo.edu.uagrm.credenciales.security.JwtService;
import org.junit.jupiter.api.Test;

class JwtConfigurationTests {
    @Test void rejectsMissingShortAndPublishedDevelopmentKeys() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService(null,60));
        for(String key:java.util.List.of("", "short-private-key",
                "change-this-development-secret-with-at-least-32-characters",
                "replace-with-a-random-secret-of-at-least-32-characters")) {
            assertThrows(IllegalArgumentException.class, () -> new JwtService(key,60));
        }
    }
}
