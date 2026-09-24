package com.pm.erp.marks.api;

import com.pm.erp.assignments.service.TeacherAssignmentService;
import com.pm.erp.auth.security.AuthenticatedUser;
import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.marks.service.ExamService;
import com.pm.erp.marks.service.MarksService;
import com.pm.erp.teachers.domain.TeacherRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Exams & Marks", description = "Exam definitions and per-student marks entry. ADMIN has full access; TEACHER is restricted to sections/subjects they're assigned to.")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MarksController {

    private final ExamService examService;
    private final MarksService marksService;
    private final TeacherAssignmentService assignmentService;
    private final TeacherRepository teacherRepo;

    @Operation(summary = "List exams")
    @GetMapping("/exams")
    public List<MarksDto.ExamResponse> listExams() {
        return examService.list();
    }

    @Operation(summary = "Create an exam", description = "ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/exams")
    @ResponseStatus(HttpStatus.CREATED)
    public MarksDto.ExamResponse createExam(@RequestBody @Valid MarksDto.ExamRequest req) {
        return examService.create(req);
    }

    @Operation(summary = "List marks for an exam/section/subject")
    @GetMapping("/marks")
    public List<MarksDto.MarkRowResponse> listMarks(
            @Parameter(description = "Exam id") @RequestParam Long examId,
            @Parameter(description = "Section id") @RequestParam Long sectionId,
            @Parameter(description = "Subject id") @RequestParam Long subjectId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        requireSectionSubjectAccess(user, sectionId, subjectId);
        return marksService.list(examId, sectionId, subjectId);
    }

    @Operation(summary = "Bulk enter marks", description = "Upserts one mark entry per student for a given exam + section + subject.")
    @PostMapping("/marks/bulk")
    public MarksDto.BulkMarksResponse bulk(
            @RequestBody @Valid MarksDto.BulkMarksRequest req,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        requireSectionSubjectAccess(user, req.sectionId(), req.subjectId());
        return marksService.bulkUpsert(req);
    }

    /**
     * ADMIN can view/enter marks for any section/subject.
     * TEACHER may only view/enter marks for a subject they are directly assigned to teach
     * in that section, OR any subject in a section where they are the class teacher.
     */
    private void requireSectionSubjectAccess(AuthenticatedUser user, Long sectionId, Long subjectId) {
        if ("ADMIN".equals(user.role())) {
            return;
        }
        Long teacherId = teacherRepo.findByUserId(user.userId())
                .orElseThrow(() -> new NotFoundException("Teacher not found for user: " + user.username()))
                .getId();
        if (!assignmentService.canAccessSectionSubject(teacherId, sectionId, subjectId)) {
            throw new AccessDeniedException("Not authorized to view/enter marks for this section/subject");
        }
    }
}
