package bo.edu.uagrm.credenciales.security;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CorsOriginsTests {
    @Test void preservesSingleOriginAndRestrictsOtherSites() {
        var config = new SecurityConfig().corsConfigurationSource("http://localhost:4302")
                .getCorsConfiguration(new MockHttpServletRequest("POST", "/api/auth/platform/login"));
        assertEquals("http://localhost:4302", config.checkOrigin("http://localhost:4302"));
        assertNull(config.checkOrigin("https://other.example.test"));
    }

    @Test void temporaryHttpsAndPrivateOriginsBothWorkWithoutAllowingOtherHosts() {
        var config = new SecurityConfig().corsConfigurationSource(
                " https://demo.example.test/, http://localhost:4302, https://demo.example.test ")
                .getCorsConfiguration(new MockHttpServletRequest("POST", "/api/auth/platform/login"));
        assertEquals(List.of("https://demo.example.test", "http://localhost:4302"), config.getAllowedOrigins());
        assertEquals("https://demo.example.test", config.checkOrigin("https://demo.example.test"));
        assertEquals("http://localhost:4302", config.checkOrigin("http://localhost:4302"));
        assertNull(config.checkOrigin("https://demo.example.test.attacker.test"));
        assertTrue(config.getAllowCredentials());
    }

    @Test void rejectsWildcardsNullOriginsCredentialsAndUrlsWithExtraParts() {
        for (String origin : List.of("*", "null", "", "https://*.example.test", "https://user:pass@example.test",
                "https://example.test/path", "https://example.test?q=1", "https://example.test#fragment",
                "file:///tmp/site", "https://example.test,", "http://localhost:0", "https://example.test:65536")) {
            assertThrows(IllegalArgumentException.class,
                    () -> new SecurityConfig().corsConfigurationSource(origin), origin);
        }
    }
}
