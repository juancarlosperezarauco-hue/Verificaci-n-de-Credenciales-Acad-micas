package bo.edu.uagrm.credenciales.portal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class PortalDtos {
    private PortalDtos() {}

    public record CreateRequest(
            @NotNull PortalItemType itemType,
            @NotBlank @Size(max = 180) String title,
            @Size(max = 1200) String description,
            Instant eventAt,
            @Size(max = 500) @Pattern(regexp = "^$|^(https?://|/).*$", message = "El enlace debe comenzar con http://, https:// o /") String link,
            @Min(0) @Max(10) int priority) {}

    public record Response(UUID id, PortalItemType itemType, String title, String description,
                           Instant eventAt, String link, int priority, Instant createdAt) {
        static Response from(PortalItem item) {
            return new Response(item.getId(), item.getItemType(), item.getTitle(), item.getDescription(),
                    item.getEventAt(), item.getLink(), item.getPriority(), item.getCreatedAt());
        }
    }
}
