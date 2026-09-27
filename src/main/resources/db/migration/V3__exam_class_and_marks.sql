-- Exams become class-scoped and carry a default max-marks value.
-- class_id is nullable at the DB level (existing exams predate this concept);
-- the API requires it for all newly created exams.
ALTER TABLE exam ADD COLUMN class_id BIGINT REFERENCES class(id);
ALTER TABLE exam ADD COLUMN max_marks INT NOT NULL DEFAULT 100;
ALTER TABLE exam ALTER COLUMN max_marks DROP DEFAULT;
CREATE INDEX idx_exam_class ON exam(class_id);
