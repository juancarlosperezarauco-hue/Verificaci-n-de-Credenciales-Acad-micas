ALTER TABLE user_mfa ADD COLUMN pending_secret VARCHAR(300);
ALTER TABLE mfa_recovery_codes ADD COLUMN used_at TIMESTAMPTZ;
UPDATE mfa_recovery_codes SET used_at=CURRENT_TIMESTAMP WHERE used;
ALTER TABLE mail_outbox ADD COLUMN terminal_at TIMESTAMPTZ;
UPDATE mail_outbox SET terminal_at=COALESCE(sent_at,CURRENT_TIMESTAMP)
 WHERE state IN ('SENT','FAILED','CANCELLED');
ALTER TABLE login_sessions ADD COLUMN signing_key_id VARCHAR(40) NOT NULL DEFAULT 'legacy';
CREATE INDEX idx_session_expiry ON login_sessions(expires_at);
CREATE INDEX idx_session_revoked ON login_sessions(revoked_at) WHERE revoked_at IS NOT NULL;
CREATE INDEX idx_recovery_used ON mfa_recovery_codes(used_at) WHERE used_at IS NOT NULL;
CREATE INDEX idx_mail_terminal ON mail_outbox(terminal_at) WHERE terminal_at IS NOT NULL;
