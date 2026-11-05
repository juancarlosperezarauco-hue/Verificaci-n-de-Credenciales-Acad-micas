ALTER TABLE student_accounts ADD COLUMN search_code VARCHAR(36);
UPDATE student_accounts SET search_code = 'EST-' || upper(replace(gen_random_uuid()::text, '-', ''));
ALTER TABLE student_accounts ALTER COLUMN search_code SET NOT NULL;
ALTER TABLE student_accounts ADD CONSTRAINT uk_student_search_code UNIQUE (search_code);
