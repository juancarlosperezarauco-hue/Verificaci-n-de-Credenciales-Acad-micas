package bo.edu.uagrm.credenciales.shared;

import java.util.List;

public record ResultPage<T>(List<T> items, long total, int page, int size) {
    public static void validate(int page, int size) {
        if (page < 0 || page > 1_000_000 || size < 1 || size > 50)
            throw new IllegalArgumentException("Paginación inválida (1–50 elementos).");
    }
}
