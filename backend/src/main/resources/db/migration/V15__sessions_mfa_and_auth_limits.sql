CREATE TABLE login_sessions (
 id UUID PRIMARY KEY,
 user_id UUID REFERENCES app_users(id),
 student_id UUID REFERENCES student_accounts(id),
 tenant_id UUID REFERENCES tenants(id),
 security_version BIGINT NOT NULL,
 mfa_verified BOOLEAN NOT NULL,
 device VARCHAR(100) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 last_seen_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL,
 revoked_at TIMESTAMPTZ,
 CHECK ((user_id IS NOT NULL AND student_id IS NULL) OR (student_id IS NOT NULL AND user_id IS NULL AND tenant_id IS NULL)),
 FOREIGN KEY (user_id,tenant_id) REFERENCES app_users(id,tenant_id)
);
CREATE INDEX idx_login_sessions_user ON login_sessions(user_id,expires_at);
CREATE INDEX idx_login_sessions_student ON login_sessions(student_id,expires_at);
CREATE TABLE user_mfa (
 user_id UUID PRIMARY KEY REFERENCES app_users(id),
 encrypted_secret VARCHAR(300),
 enabled BOOLEAN NOT NULL DEFAULT FALSE,
 generation UUID NOT NULL,
 pending_until TIMESTAMPTZ,
 last_counter BIGINT NOT NULL DEFAULT -1
);
CREATE TABLE mfa_challenges (
 id UUID PRIMARY KEY,
 user_id UUID NOT NULL REFERENCES app_users(id),
 generation UUID NOT NULL,
 token_hash VARCHAR(64) NOT NULL UNIQUE,
 expires_at TIMESTAMPTZ NOT NULL,
 attempts INTEGER NOT NULL DEFAULT 0,
 consumed BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_mfa_challenge_expiry ON mfa_challenges(expires_at);
CREATE TABLE mfa_recovery_codes (
 id UUID PRIMARY KEY,
 user_id UUID NOT NULL REFERENCES app_users(id),
 code_hash VARCHAR(64) NOT NULL UNIQUE,
 used BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_mfa_recovery_user ON mfa_recovery_codes(user_id);
CREATE TABLE auth_rate_limits (
 bucket_key VARCHAR(64) PRIMARY KEY,
 attempts INTEGER NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_auth_rate_expiry ON auth_rate_limits(expires_at);
