CREATE TABLE academic_import_batches (
 id UUID PRIMARY KEY,
 tenant_id UUID NOT NULL REFERENCES tenants(id),
 creator_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL,
 applied_at TIMESTAMPTZ,
 payload TEXT NOT NULL,
 result TEXT,
 FOREIGN KEY(creator_id,tenant_id) REFERENCES app_users(id,tenant_id)
);
CREATE INDEX idx_import_tenant ON academic_import_batches(tenant_id,created_at DESC);
