ALTER TABLE title DROP CONSTRAINT title_status_check;
ALTER TABLE title ADD CONSTRAINT title_status_check CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'));

ALTER TABLE person DROP CONSTRAINT person_status_check;
ALTER TABLE person ADD CONSTRAINT person_status_check CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'));
