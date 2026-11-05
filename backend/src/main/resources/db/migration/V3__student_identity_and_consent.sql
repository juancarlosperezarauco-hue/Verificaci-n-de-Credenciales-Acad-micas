CREATE TABLE student_accounts (
 id UUID PRIMARY KEY,
 full_name VARCHAR(180) NOT NULL,
 country VARCHAR(2) NOT NULL,
 document_type VARCHAR(30) NOT NULL,
 document_number VARCHAR(80) NOT NULL,
 email VARCHAR(180) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uk_student_identity UNIQUE (country, document_type, document_number)
);
CREATE TABLE student_consents (
 id UUID PRIMARY KEY,
 student_id UUID NOT NULL REFERENCES student_accounts(id),
 tenant_id UUID NOT NULL REFERENCES tenants(id),
 token_hash VARCHAR(64) NOT NULL UNIQUE,
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL,
 used_at TIMESTAMPTZ,
 revoked_at TIMESTAMPTZ,
 used_by UUID REFERENCES app_users(id)
);
CREATE INDEX idx_consents_student ON student_consents(student_id);
CREATE INDEX idx_consents_tenant_student ON student_consents(tenant_id, student_id);
ALTER TABLE credentials ADD COLUMN student_account_id UUID REFERENCES student_accounts(id);
CREATE INDEX idx_credentials_account ON credentials(student_account_id);
