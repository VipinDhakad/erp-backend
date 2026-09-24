package com.pm.erp.students.service;

import com.pm.erp.common.error.ConflictException;
import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.common.tenant.SchoolContext;
import com.pm.erp.students.api.StudentDto;
import com.pm.erp.students.domain.Student;
import com.pm.erp.students.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository repo;
    private final SchoolContext schoolContext;

    @Transactional(readOnly = true)
    public List<StudentDto.StudentResponse> listBySection(Long sectionId) {
        return repo.findBySectionIdOrderByRollNoAsc(sectionId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public StudentDto.StudentResponse get(Long id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    public StudentDto.StudentResponse create(StudentDto.StudentRequest req) {
        Long schoolId = schoolContext.currentSchoolId();
        if (repo.existsBySchoolIdAndAdmissionNoIgnoreCase(schoolId, req.admissionNo())) {
            throw new ConflictException("Admission number already exists: " + req.admissionNo());
        }
        Student s = Student.builder()
                .schoolId(schoolId)
                .sectionId(req.sectionId())
                .admissionNo(req.admissionNo())
                .firstName(req.firstName())
                .lastName(req.lastName())
                .dob(req.dob())
                .gender(req.gender())
                .rollNo(req.rollNo())
                .build();
        Student saved = repo.save(s);
        log.info("Created student id={} admissionNo={}", saved.getId(), saved.getAdmissionNo());
        return toDto(saved);
    }

    @Transactional
    public StudentDto.StudentResponse update(Long id, StudentDto.StudentRequest req) {
        Student s = findOrThrow(id);
        s.setSectionId(req.sectionId());
        s.setAdmissionNo(req.admissionNo());
        s.setFirstName(req.firstName());
        s.setLastName(req.lastName());
        s.setDob(req.dob());
        s.setGender(req.gender());
        s.setRollNo(req.rollNo());
        log.info("Updated student id={}", id);
        return toDto(s);
    }

    @Transactional
    public void delete(Long id) {
        Student s = findOrThrow(id);
        repo.delete(s);
        log.info("Deleted student id={}", id);
    }

    private Student findOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Student not found: " + id));
    }

    private StudentDto.StudentResponse toDto(Student s) {
        return new StudentDto.StudentResponse(
                s.getId(), s.getSectionId(), s.getAdmissionNo(), s.getFirstName(), s.getLastName(),
                s.getDob(), s.getGender(), s.getRollNo()
        );
    }
}
