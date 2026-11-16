CREATE TABLE organization_profiles (
 tenant_id UUID PRIMARY KEY REFERENCES tenants(id),
 legal_name VARCHAR(160) NOT NULL, country VARCHAR(2) NOT NULL,
 region VARCHAR(120) NOT NULL, address VARCHAR(500) NOT NULL,
 phone VARCHAR(40) NOT NULL, contact_email VARCHAR(180) NOT NULL,
 website VARCHAR(255), domain VARCHAR(180),
 tax_id VARCHAR(80) NOT NULL, education_license VARCHAR(120) NOT NULL,
 ministry_registration VARCHAR(180), accreditation VARCHAR(500),
 institution_type VARCHAR(40) NOT NULL,
 representative_name VARCHAR(180) NOT NULL,
 representative_document_type VARCHAR(30) NOT NULL,
 representative_document_number VARCHAR(80) NOT NULL,
 representative_position VARCHAR(100) NOT NULL,
 representative_email VARCHAR(180) NOT NULL,
 representative_phone VARCHAR(40) NOT NULL,
 UNIQUE(country,tax_id), UNIQUE(country,education_license)
);
CREATE TABLE organization_education_levels (
 tenant_id UUID NOT NULL REFERENCES organization_profiles(tenant_id),
 level VARCHAR(30) NOT NULL,
 PRIMARY KEY(tenant_id,level)
);
