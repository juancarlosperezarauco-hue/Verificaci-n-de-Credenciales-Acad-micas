ALTER TABLE identity_review_submissions ALTER COLUMN front_key DROP NOT NULL;
ALTER TABLE identity_review_submissions ALTER COLUMN back_key DROP NOT NULL;
ALTER TABLE identity_review_submissions ADD COLUMN retired_at TIMESTAMPTZ;
ALTER TABLE identity_review_submissions ADD COLUMN files_purged_at TIMESTAMPTZ;
UPDATE identity_review_submissions SET retired_at=COALESCE(withdrawn_at,reviewed_at)
 WHERE withdrawn_at IS NOT NULL OR status='REJECTED';
-- Start a conservative clock for V18 evidence whose profile/link may already be stale.
-- At purge time the service rechecks and protects every still-current, authorized review.
UPDATE identity_review_submissions SET retired_at=CURRENT_TIMESTAMP WHERE retired_at IS NULL;
CREATE TABLE identity_disputes (
 id UUID PRIMARY KEY, student_id UUID NOT NULL REFERENCES student_accounts(id),
 country VARCHAR(2) NOT NULL, document_type VARCHAR(30) NOT NULL, document_number VARCHAR(80) NOT NULL,
 reason VARCHAR(2000) NOT NULL, status VARCHAR(20) NOT NULL, resolution VARCHAR(2000),
 front_key VARCHAR(500), back_key VARCHAR(500), front_hash VARCHAR(64) NOT NULL, back_hash VARCHAR(64) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, resolved_at TIMESTAMPTZ, resolver_id UUID REFERENCES app_users(id),
 files_purged_at TIMESTAMPTZ, retired_at TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0,
 CHECK(status IN ('OPEN','OBSERVED','RESOLVED','REJECTED','WITHDRAWN'))
);
CREATE UNIQUE INDEX uq_identity_dispute_open ON identity_disputes(student_id) WHERE status IN ('OPEN','OBSERVED');
CREATE INDEX idx_dispute_status ON identity_disputes(status,created_at DESC);
CREATE TABLE identity_dispute_events(id UUID PRIMARY KEY,dispute_id UUID NOT NULL REFERENCES identity_disputes(id),actor_id UUID NOT NULL,action VARCHAR(40) NOT NULL,status VARCHAR(20) NOT NULL,reason VARCHAR(2000),occurred_at TIMESTAMPTZ NOT NULL);
