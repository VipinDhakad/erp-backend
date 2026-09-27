-- H2 mirror of db/migration/V3__exam_class_and_marks.sql, for the local profile only.
ALTER TABLE exam ADD COLUMN class_id BIGINT REFERENCES class(id);
ALTER TABLE exam ADD COLUMN max_marks INT NOT NULL DEFAULT 100;
ALTER TABLE exam ALTER COLUMN max_marks DROP DEFAULT;
CREATE INDEX idx_exam_class ON exam(class_id);
