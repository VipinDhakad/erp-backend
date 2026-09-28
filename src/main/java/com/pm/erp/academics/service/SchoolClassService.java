package com.pm.erp.academics.service;

import com.pm.erp.academics.api.AcademicsDto;
import com.pm.erp.academics.domain.*;
import com.pm.erp.assignments.domain.TeacherAssignment;
import com.pm.erp.assignments.domain.TeacherAssignmentRepository;
import com.pm.erp.common.error.ConflictException;
import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.common.tenant.SchoolContext;
import com.pm.erp.students.domain.StudentRepository;
import com.pm.erp.teachers.domain.Teacher;
import com.pm.erp.teachers.domain.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolClassService {

    private final SchoolClassRepository classRepo;
    private final ClassSubjectRepository classSubjectRepo;
    private final SubjectRepository subjectRepo;
    private final SectionRepository sectionRepo;
    private final StudentRepository studentRepo;
    private final TeacherAssignmentRepository assignmentRepo;
    private final TeacherRepository teacherRepo;
    private final SchoolContext schoolContext;

    @Transactional(readOnly = true)
    public List<AcademicsDto.ClassResponse> list() {
        Long schoolId = schoolContext.currentSchoolId();
        List<SchoolClass> classes = classRepo.findBySchoolIdOrderByDisplayOrderAsc(schoolId);
        List<Long> classIds = classes.stream().map(SchoolClass::getId).toList();

        List<Section> sections = sectionRepo.findByClassIdIn(classIds);
        Map<Long, List<Section>> sectionsByClass = new HashMap<>();
        sections.forEach(s -> sectionsByClass.computeIfAbsent(s.getClassId(), k -> new java.util.ArrayList<>()).add(s));

        List<Long> sectionIds = sections.stream().map(Section::getId).toList();
        List<TeacherAssignment> classTeacherAssignments = sectionIds.isEmpty()
                ? List.of()
                : assignmentRepo.findBySectionIdInAndClassTeacherTrue(sectionIds);
        Map<Long, Long> classTeacherIdBySectionId = new HashMap<>();
        classTeacherAssignments.forEach(a -> classTeacherIdBySectionId.put(a.getSectionId(), a.getTeacherId()));

        Map<Long, Teacher> teacherById = new HashMap<>();
        classTeacherAssignments.forEach(a -> teacherRepo.findById(a.getTeacherId()).ifPresent(t -> teacherById.put(t.getId(), t)));

        return classes.stream().map(c -> {
            List<Section> classSections = sectionsByClass.getOrDefault(c.getId(), List.of());
            List<Long> classSectionIds = classSections.stream().map(Section::getId).toList();
            long studentCount = classSectionIds.isEmpty() ? 0 : studentRepo.countBySectionIdIn(classSectionIds);
            String classTeacherName = classSectionIds.stream()
                    .map(classTeacherIdBySectionId::get)
                    .filter(java.util.Objects::nonNull)
                    .map(teacherById::get)
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .map(t -> t.getFirstName() + " " + t.getLastName())
                    .orElse(null);
            return new AcademicsDto.ClassResponse(c.getId(), c.getName(), classTeacherName, studentCount);
        }).toList();
    }

    @Transactional
    public AcademicsDto.ClassResponse create(AcademicsDto.ClassRequest req) {
        Long schoolId = schoolContext.currentSchoolId();
        if (classRepo.existsBySchoolIdAndNameIgnoreCase(schoolId, req.name())) {
            throw new ConflictException("Class already exists with name: " + req.name());
        }
        List<SchoolClass> existing = classRepo.findBySchoolIdOrderByDisplayOrderAsc(schoolId);
        int nextDisplayOrder = existing.isEmpty() ? 10 : existing.get(existing.size() - 1).getDisplayOrder() + 10;
        SchoolClass c = SchoolClass.builder()
                .schoolId(schoolId)
                .name(req.name())
                .displayOrder(nextDisplayOrder)
                .build();
        SchoolClass saved = classRepo.save(c);
        log.info("Created class id={} name={} displayOrder={}", saved.getId(), saved.getName(), saved.getDisplayOrder());
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
        return new AcademicsDto.ClassResponse(c.getId(), c.getName(), null, 0);
    }
}
