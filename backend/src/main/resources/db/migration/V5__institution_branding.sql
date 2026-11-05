ALTER TABLE tenants
    ADD COLUMN primary_color VARCHAR(7) NOT NULL DEFAULT '#22D3EE',
    ADD COLUMN secondary_color VARCHAR(7) NOT NULL DEFAULT '#2563EB',
    ADD COLUMN accent_color VARCHAR(7) NOT NULL DEFAULT '#A855F7',
    ADD COLUMN branding_style VARCHAR(30) NOT NULL DEFAULT 'MODERNO',
    ADD COLUMN logo_object_key VARCHAR(500),
    ADD COLUMN logo_name VARCHAR(255),
    ADD COLUMN logo_content_type VARCHAR(120),
    ADD COLUMN verification_document_object_key VARCHAR(500),
    ADD COLUMN verification_document_name VARCHAR(255),
    ADD COLUMN verification_document_content_type VARCHAR(120),
    ADD COLUMN verification_document_hash VARCHAR(64),
    ADD COLUMN signatory_name VARCHAR(180),
    ADD COLUMN signatory_title VARCHAR(180);
