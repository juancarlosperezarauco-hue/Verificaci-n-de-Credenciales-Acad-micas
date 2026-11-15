ALTER TABLE student_accounts ADD COLUMN identity_revision BIGINT NOT NULL DEFAULT 0;
CREATE TABLE identity_review_submissions (
 id UUID PRIMARY KEY,
 student_id UUID NOT NULL REFERENCES student_accounts(id),
 tenant_id UUID NOT NULL REFERENCES tenants(id),
 consent_id UUID NOT NULL REFERENCES student_consents(id),
 status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','VERIFIED','REJECTED')),
 profile_json TEXT NOT NULL,
 profile_hash VARCHAR(64) NOT NULL,
 front_key VARCHAR(300) NOT NULL,
 front_hash VARCHAR(64) NOT NULL,
 back_key VARCHAR(300) NOT NULL,
 back_hash VARCHAR(64) NOT NULL,
 photo_key VARCHAR(300),
 photo_hash VARCHAR(64),
 submitted_at TIMESTAMPTZ NOT NULL,
 accepted_at TIMESTAMPTZ NOT NULL,
 reviewed_at TIMESTAMPTZ,
 reviewer_id UUID REFERENCES app_users(id),
 reason VARCHAR(1000),
 withdrawn_at TIMESTAMPTZ,
 version BIGINT NOT NULL DEFAULT 0,
 CHECK ((status='PENDING' AND reviewed_at IS NULL AND reviewer_id IS NULL)
     OR (status IN ('VERIFIED','REJECTED') AND reviewed_at IS NOT NULL AND reviewer_id IS NOT NULL)),
 CHECK (status<>'REJECTED' OR length(trim(reason))>=10)
);
CREATE INDEX idx_identity_review_student ON identity_review_submissions(student_id,submitted_at DESC);
CREATE INDEX idx_identity_review_tenant ON identity_review_submissions(tenant_id,status,submitted_at DESC);
CREATE UNIQUE INDEX uq_identity_review_pending ON identity_review_submissions(student_id,tenant_id)
 WHERE status='PENDING' AND withdrawn_at IS NULL;
