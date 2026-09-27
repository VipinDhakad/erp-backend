package com.pm.erp.analytics.api;

import com.pm.erp.assignments.service.TeacherAssignmentService;
import com.pm.erp.analytics.service.AnalyticsService;
import com.pm.erp.auth.security.AuthenticatedUser;
import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.students.domain.Student;
import com.pm.erp.students.domain.StudentRepository;
import com.pm.erp.teachers.domain.TeacherRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
            value = """
                    {
                      "studentId": 1,
                      "overallPercentage": 89.25,
                      "attendancePercentage": 92.0,
                      "bySubject": [
                        { "subjectId": 3, "subjectName": "Mathematics", "percentage": 87.5, "grade": "A" },
                        { "subjectId": 4, "subjectName": "Science", "percentage": 91.0, "grade": "A+" }
                      ],
                      "byExam": [
                        { "examId": 1, "examName": "Unit Test 1", "percentage": 89.25 }
                      ],
                      "attendanceTrend": [
                        { "month": "2026-08", "presentPct": 90.0 },
                        { "month": "2026-09", "presentPct": 92.0 }
                      ],
                      "rankInSection": 2,
                      "sectionSize": 25
                    }
                    """
    )))
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
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
            value = """
                    {
                      "enrolmentByClass": [
                        { "className": "Class 6", "count": 40 },
                        { "className": "Class 7", "count": 38 },
                        { "className": "Class 8", "count": 50 }
                      ],
                      "avgScoreBySubject": [
                        { "subjectName": "Mathematics", "avg": 78.4 },
                        { "subjectName": "Science", "avg": 81.2 }
                      ],
                      "attendanceTrend": [
                        { "month": "2026-08", "presentPct": 90.0 },
                        { "month": "2026-09", "presentPct": 91.5 }
                      ],
                      "genderDistribution": { "male": 68, "female": 60, "other": 0 }
                    }
                    """
    )))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/school-overview")
    public AnalyticsDto.SchoolOverviewResponse schoolOverview() {
        return service.schoolOverview();
    }
}
