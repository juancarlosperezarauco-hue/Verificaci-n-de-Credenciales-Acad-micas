ALTER TABLE app_users ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE app_users ADD COLUMN full_name VARCHAR(180);
ALTER TABLE app_users ADD CONSTRAINT ck_user_scope CHECK (
 (role = 'SUPER_ADMIN' AND tenant_id IS NULL) OR
 (role IN ('ADMIN','TENANT_ADMIN','CUSTOM','ISSUER','AUDITOR') AND tenant_id IS NOT NULL)
);
CREATE UNIQUE INDEX uk_platform_email ON app_users(email) WHERE tenant_id IS NULL;
ALTER TABLE app_users ADD CONSTRAINT uk_user_id_tenant UNIQUE (id,tenant_id);
CREATE TABLE permissions (code VARCHAR(60) PRIMARY KEY);
INSERT INTO permissions(code) VALUES
 ('ORGANIZATION_VIEW'),('ORGANIZATION_UPDATE'),('USER_VIEW'),('USER_CREATE'),
 ('USER_UPDATE'),('USER_DISABLE'),('ROLE_VIEW'),('ROLE_CREATE'),('ROLE_UPDATE'),
 ('ROLE_DELETE'),('STUDENT_VIEW'),('STUDENT_LINK'),('CREDENTIAL_VIEW'),
 ('CREDENTIAL_CREATE'),('CREDENTIAL_ISSUE'),('CREDENTIAL_REVOKE'),
 ('IDENTITY_REVIEW'),('REPORT_VIEW'),('AUDIT_VIEW');
CREATE TABLE tenant_roles (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenants(id),
 name VARCHAR(100) NOT NULL,
 CONSTRAINT uk_role_id_tenant UNIQUE (id,tenant_id),
 CONSTRAINT uk_role_name UNIQUE (tenant_id,name)
);
CREATE UNIQUE INDEX uk_role_name_normalized ON tenant_roles(tenant_id,lower(name));
CREATE TABLE role_permissions (
 id UUID PRIMARY KEY, role_id UUID NOT NULL REFERENCES tenant_roles(id),
 permission_code VARCHAR(60) NOT NULL REFERENCES permissions(code),
 CONSTRAINT uk_role_permission UNIQUE (role_id,permission_code)
);
CREATE TABLE user_roles (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenants(id),
 user_id UUID NOT NULL, role_id UUID NOT NULL,
 CONSTRAINT fk_assignment_user FOREIGN KEY(user_id,tenant_id) REFERENCES app_users(id,tenant_id),
 CONSTRAINT fk_assignment_role FOREIGN KEY(role_id,tenant_id) REFERENCES tenant_roles(id,tenant_id),
 CONSTRAINT uk_user_role UNIQUE (user_id,role_id)
);
CREATE TABLE organization_governance (
 tenant_id UUID PRIMARY KEY REFERENCES tenants(id),
 status VARCHAR(30) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE organization_status_events (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenants(id),
 actor_id UUID NOT NULL REFERENCES app_users(id),
 previous_status VARCHAR(30) NOT NULL, status VARCHAR(30) NOT NULL,
 reason VARCHAR(500) NOT NULL, created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_user_roles_tenant_user ON user_roles(tenant_id,user_id);
CREATE INDEX idx_status_event_tenant ON organization_status_events(tenant_id,created_at);
INSERT INTO organization_governance(tenant_id,status)
 SELECT id, CASE WHEN active THEN 'ACTIVE' ELSE 'INACTIVE' END FROM tenants;
