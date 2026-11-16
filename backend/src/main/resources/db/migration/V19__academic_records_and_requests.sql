INSERT INTO permissions(code) VALUES ('ACADEMIC_VIEW'),('ACADEMIC_MANAGE'),('REQUEST_VIEW'),('REQUEST_REVIEW'),('REQUEST_APPROVE'),('TEMPLATE_VIEW'),('TEMPLATE_MANAGE'),('SIGNING_MANAGE'),('CREDENTIAL_SIGN');
CREATE TABLE academic_programs (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenants(id), name VARCHAR(180) NOT NULL,
 kind VARCHAR(80) NOT NULL, period VARCHAR(80) NOT NULL, requirements VARCHAR(2000), active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0, UNIQUE(id,tenant_id)
);
CREATE TABLE academic_records (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenants(id), student_id UUID NOT NULL REFERENCES student_accounts(id),
 program_id UUID NOT NULL, registration_code VARCHAR(80) NOT NULL, status VARCHAR(20) NOT NULL,
 enrolled_on DATE NOT NULL, completed_on DATE, result VARCHAR(120), notes VARCHAR(2000),
 created_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 FOREIGN KEY(program_id,tenant_id) REFERENCES academic_programs(id,tenant_id),
 UNIQUE(id,tenant_id,student_id), UNIQUE(tenant_id,student_id,program_id), UNIQUE(tenant_id,registration_code),
 CHECK(status IN ('ENROLLED','COMPLETED','WITHDRAWN')), CHECK(completed_on IS NULL OR completed_on>=enrolled_on)
);
CREATE INDEX idx_academic_record_student ON academic_records(student_id,created_at DESC);
CREATE TABLE credential_requests (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenants(id), student_id UUID NOT NULL REFERENCES student_accounts(id),
 record_id UUID NOT NULL, source_credential_id UUID REFERENCES credentials(id),
 credential_type VARCHAR(60) NOT NULL, title VARCHAR(220) NOT NULL, message VARCHAR(2000),
 status VARCHAR(20) NOT NULL, reviewer_id UUID, approver_id UUID,
 template_version_id UUID, student_revision BIGINT NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 FOREIGN KEY(record_id,tenant_id,student_id) REFERENCES academic_records(id,tenant_id,student_id),
 FOREIGN KEY(reviewer_id,tenant_id) REFERENCES app_users(id,tenant_id),
 FOREIGN KEY(approver_id,tenant_id) REFERENCES app_users(id,tenant_id),
 UNIQUE(id,tenant_id,student_id),
 CHECK(status IN ('SUBMITTED','IN_REVIEW','OBSERVED','REVIEWED','APPROVED','FULFILLED','REJECTED','WITHDRAWN')),
 CHECK(reviewer_id IS NULL OR approver_id IS NULL OR reviewer_id<>approver_id)
);
CREATE UNIQUE INDEX uq_active_correction ON credential_requests(source_credential_id)
 WHERE source_credential_id IS NOT NULL AND status NOT IN ('FULFILLED','REJECTED','WITHDRAWN');
CREATE UNIQUE INDEX uq_active_document_request ON credential_requests(record_id,credential_type)
 WHERE source_credential_id IS NULL AND status NOT IN ('FULFILLED','REJECTED','WITHDRAWN');
CREATE INDEX idx_request_tenant ON credential_requests(tenant_id,status,created_at DESC);
CREATE INDEX idx_request_student ON credential_requests(student_id,created_at DESC);
CREATE TABLE request_events (
 id UUID PRIMARY KEY, request_id UUID NOT NULL REFERENCES credential_requests(id), actor_id UUID NOT NULL,
 action VARCHAR(40) NOT NULL, status VARCHAR(20) NOT NULL, reason VARCHAR(2000), occurred_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE request_attachments (
 id UUID PRIMARY KEY, request_id UUID NOT NULL REFERENCES credential_requests(id), object_key VARCHAR(500) NOT NULL,
 content_hash VARCHAR(64) NOT NULL, content_type VARCHAR(80) NOT NULL, name VARCHAR(160) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL
);
ALTER TABLE credentials ADD COLUMN source_request_id UUID UNIQUE REFERENCES credential_requests(id);
