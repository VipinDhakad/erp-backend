package com.pm.erp.analytics.api;

import com.pm.erp.assignments.service.TeacherAssignmentService;
import com.pm.erp.analytics.service.AnalyticsService;
import com.pm.erp.auth.security.AuthenticatedUser;
import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.students.domain.Student;
import com.pm.erp.students.domain.StudentRepository;
import com.pm.erp.teachers.domain.TeacherRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analytics", description = "Computed dashboards derived from marks and attendance — per-student performance and school-wide overview")
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService service;
    private final StudentRepository studentRepo;
    private final TeacherAssignmentService assignmentService;
    private final TeacherRepository teacherRepo;

    @Operation(
            summary = "Get a student's analytics",
            description = "Overall %, attendance %, per-subject and per-exam breakdown, attendance trend, and section rank. " +
                    "ADMIN can view any student; TEACHER only students in a section they're assigned to."
    )
    @GetMapping("/students/{id}")
    public AnalyticsDto.StudentAnalyticsResponse forStudent(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        if (!"ADMIN".equals(user.role())) {
            Student student = studentRepo.findById(id)
                    .orElseThrow(() -> new NotFoundException("Student not found: " + id));
            Long teacherId = teacherRepo.findByUserId(user.userId())
                    .orElseThrow(() -> new NotFoundException("Teacher not found for user: " + user.username()))
                    .getId();
            boolean allowed = assignmentService.listForTeacher(teacherId).stream()
                    .anyMatch(a -> a.sectionId().equals(student.getSectionId()));
            if (!allowed) {
                throw new AccessDeniedException("Not authorized to view analytics for this student");
            }
        }
        return service.forStudent(id);
    }

    @Operation(summary = "School-wide overview", description = "Enrolment by class, average score by subject, attendance trend, gender distribution. ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/school-overview")
    public AnalyticsDto.SchoolOverviewResponse schoolOverview() {
        return service.schoolOverview();
    }
}
