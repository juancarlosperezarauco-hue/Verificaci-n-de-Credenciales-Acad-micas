CREATE TABLE portal_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    item_type VARCHAR(30) NOT NULL,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(1200),
    event_at TIMESTAMPTZ,
    link VARCHAR(500),
    priority INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_portal_items_tenant_type ON portal_items(tenant_id, item_type, active);
