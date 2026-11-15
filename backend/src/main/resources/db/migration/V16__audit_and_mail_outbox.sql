CREATE TABLE audit_events (
 id UUID PRIMARY KEY,
 occurred_at TIMESTAMPTZ NOT NULL,
 tenant_id UUID REFERENCES tenants(id),
 actor_id UUID,
 actor_type VARCHAR(16) NOT NULL,
 actor_role VARCHAR(40) NOT NULL,
 action VARCHAR(60) NOT NULL,
 resource_type VARCHAR(30) NOT NULL,
 resource_id UUID NOT NULL,
 correlation_id UUID NOT NULL,
 changes TEXT,
 CHECK ((actor_type='SYSTEM' AND actor_id IS NULL) OR (actor_type IN ('USER','STUDENT') AND actor_id IS NOT NULL))
);
CREATE INDEX idx_audit_tenant_time ON audit_events(tenant_id,occurred_at DESC,id DESC);
CREATE INDEX idx_audit_time ON audit_events(occurred_at DESC,id DESC);
CREATE INDEX idx_audit_resource ON audit_events(resource_id,occurred_at DESC);
CREATE FUNCTION reject_audit_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 RAISE EXCEPTION 'Audit events are append-only';
END;
$$;
CREATE TRIGGER audit_events_append_only BEFORE UPDATE OR DELETE ON audit_events
 FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();

ALTER TABLE student_account_tokens ADD CONSTRAINT uq_student_token_owner UNIQUE(id,student_id);
CREATE TABLE mail_outbox (
 id UUID PRIMARY KEY,
 student_id UUID NOT NULL REFERENCES student_accounts(id),
 token_id UUID NOT NULL,
 kind VARCHAR(30) NOT NULL,
 state VARCHAR(16) NOT NULL,
 encrypted_payload TEXT,
 attempts INTEGER NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL,
 next_attempt_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL,
 claim_id UUID,
 lease_until TIMESTAMPTZ,
 sent_at TIMESTAMPTZ,
 last_error VARCHAR(40),
 FOREIGN KEY(token_id,student_id) REFERENCES student_account_tokens(id,student_id),
 CHECK (state IN ('PENDING','PROCESSING','RETRY','SENT','FAILED','CANCELLED')),
 CHECK (attempts>=0)
);
CREATE INDEX idx_mail_due ON mail_outbox(state,next_attempt_at);
CREATE INDEX idx_mail_lease ON mail_outbox(state,lease_until);
CREATE INDEX idx_mail_created ON mail_outbox(created_at DESC,id DESC);
