CREATE TABLE academic_notifications (
 id UUID PRIMARY KEY,
 student_id UUID NOT NULL REFERENCES student_accounts(id),
 tenant_id UUID NOT NULL REFERENCES tenants(id),
 event_key VARCHAR(160) NOT NULL,
 kind VARCHAR(40) NOT NULL,
 resource_id UUID NOT NULL,
 title VARCHAR(180) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 read_at TIMESTAMPTZ,
 UNIQUE(student_id,event_key)
);
CREATE INDEX idx_notification_student_time ON academic_notifications(student_id,created_at DESC,id DESC);
CREATE INDEX idx_notification_student_unread ON academic_notifications(student_id,read_at);
CREATE INDEX idx_credential_history ON credentials(student_account_id,issued_at DESC,id DESC);
CREATE INDEX idx_credential_reporting ON credentials(tenant_id,issued_at,status,credential_type);
ALTER TABLE mail_outbox ALTER COLUMN token_id DROP NOT NULL;
ALTER TABLE mail_outbox ADD CONSTRAINT ck_mail_token_kind CHECK
 ((kind='ACADEMIC' AND token_id IS NULL) OR (kind<>'ACADEMIC' AND token_id IS NOT NULL));
