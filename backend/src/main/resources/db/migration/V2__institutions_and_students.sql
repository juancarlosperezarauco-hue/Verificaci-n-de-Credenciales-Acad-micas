ALTER TABLE tenants
    ADD COLUMN institution_type VARCHAR(40) NOT NULL DEFAULT 'OTRO';

CREATE TABLE students (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    full_name VARCHAR(180) NOT NULL,
    document_number VARCHAR(80) NOT NULL,
    email VARCHAR(180),
    phone VARCHAR(40),
    program VARCHAR(220),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_student_tenant_document UNIQUE (tenant_id, document_number)
);

CREATE INDEX idx_students_tenant ON students(tenant_id);

ALTER TABLE credentials
    ADD COLUMN student_id UUID REFERENCES students(id);

CREATE INDEX idx_credentials_student ON credentials(student_id);
