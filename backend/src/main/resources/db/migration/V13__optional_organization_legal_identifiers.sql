ALTER TABLE organization_profiles ALTER COLUMN tax_id DROP NOT NULL;
ALTER TABLE organization_profiles ALTER COLUMN education_license DROP NOT NULL;

UPDATE organization_profiles SET tax_id = NULL WHERE trim(tax_id) = '';
UPDATE organization_profiles SET education_license = NULL WHERE trim(education_license) = '';
