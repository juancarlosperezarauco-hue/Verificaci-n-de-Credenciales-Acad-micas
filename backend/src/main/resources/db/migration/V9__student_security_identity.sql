ALTER TABLE student_accounts ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE student_accounts ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE student_accounts ADD COLUMN deactivated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE student_accounts ADD COLUMN identity_status VARCHAR(30) NOT NULL DEFAULT 'UNVERIFIED';
ALTER TABLE student_accounts ADD COLUMN identity_rejection_reason VARCHAR(500);
ALTER TABLE student_accounts ADD COLUMN photo_object_key VARCHAR(500);
ALTER TABLE student_accounts ADD COLUMN photo_name VARCHAR(255);
ALTER TABLE student_accounts ADD COLUMN photo_content_type VARCHAR(120);
ALTER TABLE student_accounts ADD COLUMN ci_front_object_key VARCHAR(500);
ALTER TABLE student_accounts ADD COLUMN ci_front_name VARCHAR(255);
ALTER TABLE student_accounts ADD COLUMN ci_front_content_type VARCHAR(120);
ALTER TABLE student_accounts ADD COLUMN ci_back_object_key VARCHAR(500);
ALTER TABLE student_accounts ADD COLUMN ci_back_name VARCHAR(255);
ALTER TABLE student_accounts ADD COLUMN ci_back_content_type VARCHAR(120);

CREATE TABLE student_account_tokens (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES student_accounts(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    token_type VARCHAR(30) NOT NULL,
    pending_email VARCHAR(180),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_student_tokens_student_type ON student_account_tokens(student_id, token_type);
