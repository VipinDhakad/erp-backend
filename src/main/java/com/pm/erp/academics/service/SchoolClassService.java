package com.pm.erp.academics.service;

import com.pm.erp.academics.api.AcademicsDto;
import com.pm.erp.academics.domain.*;
import com.pm.erp.common.error.ConflictException;
import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.common.tenant.SchoolContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolClassService {

    private final SchoolClassRepository classRepo;
    private final ClassSubjectRepository classSubjectRepo;
    private final SubjectRepository subjectRepo;
    private final SchoolContext schoolContext;

    @Transactional(readOnly = true)
    public List<AcademicsDto.ClassResponse> list() {
        Long schoolId = schoolContext.currentSchoolId();
        return classRepo.findBySchoolIdOrderByDisplayOrderAsc(schoolId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AcademicsDto.ClassResponse create(AcademicsDto.ClassRequest req) {
        Long schoolId = schoolContext.currentSchoolId();
        if (classRepo.existsBySchoolIdAndNameIgnoreCase(schoolId, req.name())) {
            throw new ConflictException("Class already exists with name: " + req.name());
        }
        SchoolClass c = SchoolClass.builder()
                .schoolId(schoolId)
                .name(req.name())
                .displayOrder(req.displayOrder())
                .build();
        SchoolClass saved = classRepo.save(c);
        log.info("Created class id={} name={}", saved.getId(), saved.getName());
        return toDto(saved);
    }

    @Transactional
    public void assignSubjects(Long classId, AcademicsDto.AssignSubjectsRequest req) {
        SchoolClass schoolClass = classRepo.findById(classId)
                .orElseThrow(() -> new NotFoundException("Class not found: " + classId));
        Long schoolId = schoolContext.currentSchoolId();
        if (!schoolClass.getSchoolId().equals(schoolId)) {
            throw new NotFoundException("Class not found: " + classId);
        }
        classSubjectRepo.deleteByClassId(classId);
        req.subjectIds().forEach(subjectId -> {
            if (!subjectRepo.existsById(subjectId)) {
                throw new NotFoundException("Subject not found: " + subjectId);
            }
            classSubjectRepo.save(new ClassSubject(classId, subjectId));
        });
        log.info("Assigned {} subject(s) to classId={}", req.subjectIds().size(), classId);
    }

    private AcademicsDto.ClassResponse toDto(SchoolClass c) {
        return new AcademicsDto.ClassResponse(c.getId(), c.getName(), c.getDisplayOrder());
    }
}
