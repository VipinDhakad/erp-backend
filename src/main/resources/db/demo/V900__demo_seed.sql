-- Demo profile seed. Loaded only when spring.profiles.active=demo.
-- Credentials:
--   admin     / admin123
--   teacher1  / teacher123
--   teacher2  / teacher123

INSERT INTO school (name, code, address, phone, principal_name)
VALUES ('Sunrise Public School', 'SPS-001',
        'M.G. Road, Pune, Maharashtra 411001',
        '+91-20-1234-5678',
        'Mrs. Sunita Verma');

INSERT INTO academic_year (school_id, name, start_date, end_date, is_current)
VALUES ((SELECT id FROM school WHERE code = 'SPS-001'),
        '2026-27', DATE '2026-04-01', DATE '2027-03-31', TRUE);

INSERT INTO class (school_id, name, display_order) VALUES
    ((SELECT id FROM school WHERE code = 'SPS-001'), 'Class 6', 6),
    ((SELECT id FROM school WHERE code = 'SPS-001'), 'Class 7', 7),
    ((SELECT id FROM school WHERE code = 'SPS-001'), 'Class 8', 8);

INSERT INTO section (class_id, academic_year_id, name)
SELECT c.id, ay.id, s.name
FROM class c
CROSS JOIN (VALUES ('A'), ('B')) AS s(name)
JOIN academic_year ay ON ay.school_id = c.school_id AND ay.is_current = TRUE
WHERE c.school_id = (SELECT id FROM school WHERE code = 'SPS-001');

INSERT INTO subject (school_id, name, code, max_marks) VALUES
    ((SELECT id FROM school WHERE code = 'SPS-001'), 'English',         'ENG', 100),
    ((SELECT id FROM school WHERE code = 'SPS-001'), 'Hindi',           'HIN', 100),
    ((SELECT id FROM school WHERE code = 'SPS-001'), 'Mathematics',     'MAT', 100),
    ((SELECT id FROM school WHERE code = 'SPS-001'), 'Science',         'SCI', 100),
    ((SELECT id FROM school WHERE code = 'SPS-001'), 'Social Science',  'SST', 100),
    ((SELECT id FROM school WHERE code = 'SPS-001'), 'Computer',        'CMP', 100);

-- Map every class to every subject (single-tenant MVP).
INSERT INTO class_subject (class_id, subject_id)
SELECT c.id, s.id
FROM class c, subject s
WHERE c.school_id = s.school_id
  AND c.school_id = (SELECT id FROM school WHERE code = 'SPS-001');

-- Users: admin + 2 teachers. bcrypt-10 hashes generated externally.
INSERT INTO app_user (school_id, username, password_hash, enabled, role_id) VALUES
    ((SELECT id FROM school WHERE code = 'SPS-001'),
     'admin',
     '$2a$10$ov9sr.sHUZ2tJSDvpkXOMuBY4UpuPLhdxjIBC9Du.fTWBmBmATNYS',
     TRUE,
     (SELECT id FROM role WHERE name = 'ADMIN')),
    ((SELECT id FROM school WHERE code = 'SPS-001'),
     'teacher1',
     '$2a$10$vt1.esJt/408ueqqFx2q/u2108fMu.PwUGoS85HhHhclKQxLboARu',
     TRUE,
     (SELECT id FROM role WHERE name = 'TEACHER')),
    ((SELECT id FROM school WHERE code = 'SPS-001'),
     'teacher2',
     '$2a$10$vt1.esJt/408ueqqFx2q/u2108fMu.PwUGoS85HhHhclKQxLboARu',
     TRUE,
     (SELECT id FROM role WHERE name = 'TEACHER'));

INSERT INTO teacher (school_id, user_id, first_name, last_name, employee_no) VALUES
    ((SELECT id FROM school WHERE code = 'SPS-001'),
     (SELECT id FROM app_user WHERE username = 'teacher1'),
     'Meera', 'Sharma', 'EMP-101'),
    ((SELECT id FROM school WHERE code = 'SPS-001'),
     (SELECT id FROM app_user WHERE username = 'teacher2'),
     'Rajesh', 'Kulkarni', 'EMP-102');

-- 5 students per section in Class 8.
INSERT INTO student (school_id, section_id, admission_no, first_name, last_name, dob, gender, roll_no)
SELECT (SELECT id FROM school WHERE code = 'SPS-001'),
       sec.id,
       v.admission_no, v.first_name, v.last_name, v.dob, v.gender, v.roll_no
FROM section sec
JOIN class c ON c.id = sec.class_id
CROSS JOIN (VALUES
    ('A-8A-001', 'Aarav',     'Sharma',  DATE '2011-04-15', 'M', 1),
    ('A-8A-002', 'Diya',      'Patel',   DATE '2011-06-21', 'F', 2),
    ('A-8A-003', 'Kabir',     'Singh',   DATE '2011-02-09', 'M', 3),
    ('A-8A-004', 'Ananya',    'Reddy',   DATE '2011-09-30', 'F', 4),
    ('A-8A-005', 'Mohammed',  'Khan',    DATE '2011-12-11', 'M', 5)
) AS v(admission_no, first_name, last_name, dob, gender, roll_no)
WHERE c.name = 'Class 8' AND sec.name = 'A';

INSERT INTO student (school_id, section_id, admission_no, first_name, last_name, dob, gender, roll_no)
SELECT (SELECT id FROM school WHERE code = 'SPS-001'),
       sec.id,
       v.admission_no, v.first_name, v.last_name, v.dob, v.gender, v.roll_no
FROM section sec
JOIN class c ON c.id = sec.class_id
CROSS JOIN (VALUES
    ('A-8B-001', 'Ishaan',    'Verma',   DATE '2011-03-18', 'M', 1),
    ('A-8B-002', 'Saanvi',    'Joshi',   DATE '2011-07-02', 'F', 2),
    ('A-8B-003', 'Arjun',     'Menon',   DATE '2011-11-05', 'M', 3),
    ('A-8B-004', 'Riya',      'Nair',    DATE '2011-05-23', 'F', 4),
    ('A-8B-005', 'Vihaan',    'Iyer',    DATE '2011-01-30', 'M', 5)
) AS v(admission_no, first_name, last_name, dob, gender, roll_no)
WHERE c.name = 'Class 8' AND sec.name = 'B';

-- One exam in current academic year.
INSERT INTO exam (school_id, academic_year_id, name, start_date, end_date)
VALUES ((SELECT id FROM school WHERE code = 'SPS-001'),
        (SELECT id FROM academic_year WHERE is_current = TRUE
            AND school_id = (SELECT id FROM school WHERE code = 'SPS-001')),
        'Unit Test 1', DATE '2026-07-15', DATE '2026-07-22');

-- Teacher assignments: teacher1 (Meera Sharma) is the class teacher of Class 8 - A
-- (sees all subjects for that section); teacher2 (Rajesh Kulkarni) teaches only
-- Mathematics and Science in Class 8 - B (subject-scoped, not a class teacher).
INSERT INTO teacher_assignment (teacher_id, section_id, subject_id, is_class_teacher)
SELECT t.id, sec.id, NULL, TRUE
FROM teacher t
JOIN section sec ON sec.name = 'A'
JOIN class c ON c.id = sec.class_id AND c.name = 'Class 8'
WHERE t.employee_no = 'EMP-101';

INSERT INTO teacher_assignment (teacher_id, section_id, subject_id, is_class_teacher)
SELECT t.id, sec.id, subj.id, FALSE
FROM teacher t
JOIN section sec ON sec.name = 'B'
JOIN class c ON c.id = sec.class_id AND c.name = 'Class 8'
JOIN subject subj ON subj.code IN ('MAT', 'SCI') AND subj.school_id = c.school_id
WHERE t.employee_no = 'EMP-102';
