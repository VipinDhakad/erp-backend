package com.pm.erp.attendance.api;

import com.pm.erp.assignments.service.TeacherAssignmentService;
import com.pm.erp.auth.security.AuthenticatedUser;
import com.pm.erp.attendance.service.AttendanceService;
import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.teachers.domain.TeacherRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService service;
    private final TeacherAssignmentService assignmentService;
    private final TeacherRepository teacherRepo;

    @GetMapping
    public List<AttendanceDto.AttendanceRowResponse> list(
            @RequestParam Long sectionId,
            @RequestParam LocalDate date,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        requireSectionAccess(user, sectionId);
        return service.list(sectionId, date);
    }

    @PostMapping("/bulk")
    public AttendanceDto.BulkAttendanceResponse bulk(
            @RequestBody @Valid AttendanceDto.BulkAttendanceRequest req,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        requireSectionAccess(user, req.sectionId());
        return service.bulkUpsert(req);
    }

    @GetMapping("/trend")
    public List<AttendanceDto.DayAttendance> trend(
            @RequestParam Long sectionId,
            @RequestParam(defaultValue = "14") int days,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        requireSectionAccess(user, sectionId);
        return service.sectionTrend(sectionId, days);
    }

    private void requireSectionAccess(AuthenticatedUser user, Long sectionId) {
        if ("ADMIN".equals(user.role())) {
            return;
        }
        Long teacherId = teacherRepo.findByUserId(user.userId())
                .orElseThrow(() -> new NotFoundException("Teacher not found for user: " + user.username()))
                .getId();
        boolean allowed = assignmentService.listForTeacher(teacherId).stream()
                .anyMatch(a -> a.sectionId().equals(sectionId));
        if (!allowed) {
            throw new AccessDeniedException("Not authorized to view/edit attendance for this section");
        }
    }
}
