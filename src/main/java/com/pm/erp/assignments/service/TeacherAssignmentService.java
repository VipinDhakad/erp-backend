package com.pm.erp.assignments.service;

import com.pm.erp.assignments.api.AssignmentDto;
import com.pm.erp.assignments.domain.TeacherAssignment;
import com.pm.erp.assignments.domain.TeacherAssignmentRepository;
import com.pm.erp.auth.domain.AppUser;
import com.pm.erp.auth.domain.AppUserRepository;
import com.pm.erp.common.error.ConflictException;
import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.common.tenant.SchoolContext;
import com.pm.erp.teachers.domain.Teacher;
import com.pm.erp.teachers.domain.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherAssignmentService {

    private final TeacherAssignmentRepository repo;
    private final TeacherRepository teacherRepo;
    private final AppUserRepository userRepo;
    private final SchoolContext schoolContext;

    @Transactional(readOnly = true)
    public List<AssignmentDto.TeacherAssignmentResponse> list() {
        Long schoolId = schoolContext.currentSchoolId();
        Set<Long> schoolTeacherIds = teacherRepo.findBySchoolIdOrderByLastNameAscFirstNameAsc(schoolId).stream()
                .map(Teacher::getId).collect(java.util.stream.Collectors.toSet());
        return repo.findAllByOrderBySectionIdAsc().stream()
                .filter(a -> schoolTeacherIds.contains(a.getTeacherId()))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssignmentDto.TeacherAssignmentResponse> listForTeacherUsername(String username) {
        AppUser user = userRepo.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        Teacher teacher = teacherRepo.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Teacher not found for user: " + username));
        return listForTeacher(teacher.getId());
    }

    @Transactional(readOnly = true)
    public List<AssignmentDto.TeacherAssignmentResponse> listForTeacher(Long teacherId) {
        return repo.findByTeacherId(teacherId).stream().map(this::toDto).toList();
    }

    @Transactional
    public AssignmentDto.TeacherAssignmentResponse create(AssignmentDto.TeacherAssignmentRequest req) {
        if (repo.existsByTeacherIdAndSectionIdAndSubjectId(req.teacherId(), req.sectionId(), req.subjectId())) {
            throw new ConflictException("Assignment already exists for this teacher/section/subject");
        }
        TeacherAssignment saved = repo.save(TeacherAssignment.builder()
                .teacherId(req.teacherId())
                .sectionId(req.sectionId())
                .subjectId(req.subjectId())
                .classTeacher(req.isClassTeacher())
                .build());
        log.info("Created teacher assignment id={} teacherId={} sectionId={}", saved.getId(), saved.getTeacherId(), saved.getSectionId());
        return toDto(saved);
    }

    @Transactional
    public void remove(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Teacher assignment not found: " + id);
        }
        repo.deleteById(id);
    }

    /** Sections a teacher may view marks/attendance for as class teacher (all subjects). */
    @Transactional(readOnly = true)
    public List<Long> classTeacherSectionIds(Long teacherId) {
        return repo.findByTeacherId(teacherId).stream()
                .filter(TeacherAssignment::isClassTeacher)
                .map(TeacherAssignment::getSectionId)
                .distinct()
                .toList();
    }

    /** True if this teacher may view/enter the given subject for the given section:
     *  either they are the class teacher of that section, or they have a direct
     *  subject-scoped assignment for it. */
    @Transactional(readOnly = true)
    public boolean canAccessSectionSubject(Long teacherId, Long sectionId, Long subjectId) {
        return repo.findByTeacherId(teacherId).stream().anyMatch(a ->
                a.getSectionId().equals(sectionId) &&
                        (a.isClassTeacher() || Objects.equals(a.getSubjectId(), subjectId))
        );
    }

    private AssignmentDto.TeacherAssignmentResponse toDto(TeacherAssignment a) {
        return new AssignmentDto.TeacherAssignmentResponse(
                a.getId(), a.getTeacherId(), a.getSectionId(), a.getSubjectId(), a.isClassTeacher());
    }
}
