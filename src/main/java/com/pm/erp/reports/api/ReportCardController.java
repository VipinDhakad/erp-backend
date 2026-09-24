package com.pm.erp.reports.api;

import com.pm.erp.assignments.service.TeacherAssignmentService;
import com.pm.erp.auth.security.AuthenticatedUser;
import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.reports.service.ReportCardService;
import com.pm.erp.students.domain.Student;
import com.pm.erp.students.domain.StudentRepository;
import com.pm.erp.teachers.domain.TeacherRepository;
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

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportCardController {

    private final ReportCardService service;
    private final StudentRepository studentRepo;
    private final TeacherAssignmentService assignmentService;
    private final TeacherRepository teacherRepo;

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
