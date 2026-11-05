package bo.edu.uagrm.credenciales.portal;

import bo.edu.uagrm.credenciales.tenant.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "portal_items")
public class PortalItem {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private PortalItemType itemType;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 1200)
    private String description;

    @Column(name = "event_at")
    private Instant eventAt;

    @Column(length = 500)
    private String link;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PortalItem() {}

    public PortalItem(Tenant tenant, PortalItemType itemType, String title, String description,
                      Instant eventAt, String link, int priority) {
        this.id = UUID.randomUUID();
        this.tenant = tenant;
        this.itemType = itemType;
        this.title = title.trim();
        this.description = blankToNull(description);
        this.eventAt = eventAt;
        this.link = blankToNull(link);
        this.priority = priority;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public void deactivate() { this.active = false; }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public UUID getId() { return id; }
    public Tenant getTenant() { return tenant; }
    public PortalItemType getItemType() { return itemType; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Instant getEventAt() { return eventAt; }
    public String getLink() { return link; }
    public int getPriority() { return priority; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
