package com.pm.erp.reports.api;

import com.pm.erp.assignments.service.TeacherAssignmentService;
import com.pm.erp.auth.security.AuthenticatedUser;
import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.reports.service.ReportCardService;
import com.pm.erp.students.domain.Student;
import com.pm.erp.students.domain.StudentRepository;
import com.pm.erp.teachers.domain.TeacherRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Report Cards", description = "Per-student, per-exam report card rendering (PDF and the underlying data)")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportCardController {

    private final ReportCardService service;
    private final StudentRepository studentRepo;
    private final TeacherAssignmentService assignmentService;
    private final TeacherRepository teacherRepo;

    @Operation(
            summary = "Render report card PDF",
            description = "Generates and returns the report card as a PDF for inline viewing."
    )
    @ApiResponse(responseCode = "200", description = "PDF report card", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @GetMapping("/student/{studentId}/exam/{examId}")
    public ResponseEntity<byte[]> render(
            @PathVariable Long studentId, @PathVariable Long examId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        requireStudentAccess(user, studentId);
        byte[] pdf = service.render(studentId, examId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"report-card-" + studentId + "-" + examId + ".pdf\"")
                .body(pdf);
    }

    @Operation(summary = "Get report card data", description = "The same computed report card as JSON, for rendering client-side instead of the PDF.")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
            value = """
                    {
                      "student": {
                        "id": 1,
                        "sectionId": 5,
                        "admissionNo": "A-8A-001",
                        "firstName": "Aarav",
                        "lastName": "Sharma",
                        "dob": "2011-04-15",
                        "gender": "M",
                        "rollNo": 1
                      },
                      "exam": {
                        "id": 1,
                        "name": "Unit Test 1",
                        "startDate": "2026-07-15",
                        "endDate": "2026-07-22",
                        "academicYearId": 1
                      },
                      "className": "Class 8",
                      "sectionName": "A",
                      "schoolName": "Sunrise Public School",
                      "academicYear": "2026-27",
                      "rows": [
                        { "subjectId": 3, "subjectName": "Mathematics", "maxMarks": 100, "marksObtained": 87.5, "grade": "A" },
                        { "subjectId": 4, "subjectName": "Science", "maxMarks": 100, "marksObtained": 91.0, "grade": "A+" }
                      ],
                      "totalMarks": 178.5,
                      "totalMaxMarks": 200,
                      "percentage": 89.25,
                      "overallGrade": "A",
                      "attendancePercentage": 92.0,
                      "remarks": "Excellent performance",
                      "rankInSection": 2,
                      "sectionSize": 25
                    }
                    """
    )))
    @GetMapping("/student/{studentId}/exam/{examId}/data")
    public ReportCardDto.ReportCardResponse data(
            @PathVariable Long studentId, @PathVariable Long examId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        requireStudentAccess(user, studentId);
        return service.buildReportCard(studentId, examId);
    }

    private void requireStudentAccess(AuthenticatedUser user, Long studentId) {
        if ("ADMIN".equals(user.role())) {
            return;
        }
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));
        Long teacherId = teacherRepo.findByUserId(user.userId())
                .orElseThrow(() -> new NotFoundException("Teacher not found for user: " + user.username()))
                .getId();
        boolean allowed = assignmentService.listForTeacher(teacherId).stream()
                .anyMatch(a -> a.sectionId().equals(student.getSectionId()));
        if (!allowed) {
            throw new AccessDeniedException("Not authorized to view this student's report card");
        }
    }
}
