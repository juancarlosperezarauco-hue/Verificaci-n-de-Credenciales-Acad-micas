CREATE TABLE document_templates (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenants(id), family_id UUID NOT NULL, version_number INT NOT NULL,
 name VARCHAR(140) NOT NULL, credential_type VARCHAR(60) NOT NULL, heading VARCHAR(180) NOT NULL,
 body_text VARCHAR(3000) NOT NULL, orientation VARCHAR(20) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL, UNIQUE(id,tenant_id), UNIQUE(family_id,version_number)
);
CREATE INDEX idx_template_tenant ON document_templates(tenant_id,created_at DESC);
ALTER TABLE credential_requests ADD CONSTRAINT fk_request_template FOREIGN KEY(template_version_id,tenant_id) REFERENCES document_templates(id,tenant_id);
CREATE TABLE institutional_signers (
 tenant_id UUID PRIMARY KEY REFERENCES tenants(id), encrypted_key TEXT NOT NULL,
 certificate_der TEXT NOT NULL, fingerprint VARCHAR(64) NOT NULL, subject VARCHAR(500) NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL, test_only BOOLEAN NOT NULL, required BOOLEAN NOT NULL DEFAULT FALSE,
 configured_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0
);
ALTER TABLE credentials ADD COLUMN template_version_id UUID;
ALTER TABLE credentials ADD CONSTRAINT fk_credential_template FOREIGN KEY(template_version_id,tenant_id) REFERENCES document_templates(id,tenant_id);
ALTER TABLE credentials ADD COLUMN signer_fingerprint VARCHAR(64);
ALTER TABLE credentials ADD COLUMN signed_at TIMESTAMPTZ;
ALTER TABLE credentials ADD COLUMN signature_test_only BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE credentials ADD COLUMN signing_certificate TEXT;
ALTER TABLE credentials ADD COLUMN issue_operation_key VARCHAR(100);
CREATE UNIQUE INDEX uq_credential_issue_key ON credentials(tenant_id,issue_operation_key) WHERE issue_operation_key IS NOT NULL;
ALTER TABLE credentials ADD CONSTRAINT uq_credential_tenant UNIQUE(id,tenant_id);
ALTER TABLE credential_requests ADD CONSTRAINT fk_correction_source_tenant FOREIGN KEY(source_credential_id,tenant_id) REFERENCES credentials(id,tenant_id);
ALTER TABLE credentials ADD CONSTRAINT fk_credential_request_owner FOREIGN KEY(source_request_id,tenant_id,student_account_id) REFERENCES credential_requests(id,tenant_id,student_id);
ALTER TABLE credentials ADD COLUMN replaces_id UUID;
ALTER TABLE credentials ADD COLUMN replaced_by_id UUID;
ALTER TABLE credentials ADD CONSTRAINT fk_credential_replaces FOREIGN KEY(replaces_id,tenant_id) REFERENCES credentials(id,tenant_id);
ALTER TABLE credentials ADD CONSTRAINT fk_credential_replaced_by FOREIGN KEY(replaced_by_id,tenant_id) REFERENCES credentials(id,tenant_id);
CREATE UNIQUE INDEX uq_credential_replacement ON credentials(replaces_id) WHERE replaces_id IS NOT NULL;
CREATE TABLE managed_objects (
 id UUID PRIMARY KEY, object_key VARCHAR(500) NOT NULL UNIQUE, owner_id UUID NOT NULL, tenant_id UUID,
 purpose VARCHAR(30) NOT NULL, content_hash VARCHAR(64) NOT NULL, size_bytes BIGINT NOT NULL,
 key_id VARCHAR(40) NOT NULL, state VARCHAR(20) NOT NULL, created_at TIMESTAMPTZ NOT NULL,
 safe_after TIMESTAMPTZ NOT NULL, delete_requested_at TIMESTAMPTZ,
 CHECK(state IN ('STAGED','BOUND','ABANDONED','REMOVED'))
);
CREATE INDEX idx_managed_cleanup ON managed_objects(state,safe_after);
