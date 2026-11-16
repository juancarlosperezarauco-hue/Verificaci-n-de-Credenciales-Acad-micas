package bo.edu.uagrm.credenciales.credential;

import java.time.*;
import java.util.*;

/** Validated, bound filters shared by history and institutional reporting. */
public record CredentialFilters(Integer year, String type, CredentialStatus status) {
    public CredentialFilters {
        if (year != null && (year < 1900 || year > 2100))
            throw new IllegalArgumentException("El año debe estar entre 1900 y 2100.");
        if (type != null) {
            type = type.strip().toUpperCase(Locale.ROOT);
            if (type.isEmpty()) type = null;
            else if (!Set.of("DIPLOMA", "CERTIFICADO", "TITULO", "RECONOCIMIENTO", "CONSTANCIA")
                    .contains(type))
                throw new IllegalArgumentException("Tipo de credencial no válido.");
        }
    }

    public Instant start() {
        return year == null
                ? null
                : LocalDate.of(year, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public Instant end() {
        return year == null
                ? null
                : LocalDate.of(year + 1, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
