package bo.edu.uagrm.credenciales.security;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Reject ambiguous or overridden TLS settings before a datasource is instantiated. */
final class PostgresTlsPolicy {
    private PostgresTlsPolicy() {}

    static void validate(String jdbcUrl) {
        try {
            if (!jdbcUrl.startsWith("jdbc:postgresql://")) throw invalid();
            URI uri = URI.create(jdbcUrl.substring(5));
            if (uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getRawFragment() != null || uri.getRawPath() == null
                    || uri.getRawPath().length() < 2 || uri.getPort() == 0
                    || uri.getPort() > 65535 || uri.getRawQuery() == null) throw invalid();
            Map<String, String> parameters = new HashMap<>();
            for (String pair : uri.getRawQuery().split("&", -1)) {
                String[] parts = pair.split("=", 2);
                if (parts.length != 2) throw invalid();
                String name = decode(parts[0]), value = decode(parts[1]);
                if (name.isBlank() || parameters.putIfAbsent(name, value) != null) throw invalid();
            }
            if (!"verify-full".equals(parameters.get("sslmode"))
                    || !"/run/certs/postgres-root.crt".equals(parameters.get("sslrootcert"))
                    || parameters.containsKey("sslhostnameverifier")
                    || (parameters.containsKey("sslfactory")
                        && !"org.postgresql.ssl.LibPQFactory".equals(parameters.get("sslfactory")))
                    || (parameters.containsKey("ssl") && !"true".equals(parameters.get("ssl")))) throw invalid();
        } catch (IllegalArgumentException malformed) {
            // Do not echo URLs, database names, query values or credentials.
            throw invalid();
        }
    }

    private static String decode(String input) {
        return URLDecoder.decode(input, StandardCharsets.UTF_8);
    }

    private static IllegalStateException invalid() {
        return new IllegalStateException(
                "Configura DB_URL PostgreSQL con sslmode=verify-full, "
                        + "sslrootcert=/run/certs/postgres-root.crt y sin opciones TLS contradictorias.");
    }
}
