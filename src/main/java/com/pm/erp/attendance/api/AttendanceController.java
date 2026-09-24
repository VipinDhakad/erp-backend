package com.pm.erp.attendance.api;

import com.pm.erp.assignments.service.TeacherAssignmentService;
import com.pm.erp.auth.security.AuthenticatedUser;
import com.pm.erp.attendance.service.AttendanceService;
import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.teachers.domain.TeacherRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Attendance", description = "Daily per-section attendance. ADMIN has full access; TEACHER is restricted to their assigned sections.")
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService service;
    private final TeacherAssignmentService assignmentService;
    private final TeacherRepository teacherRepo;

    @Operation(summary = "List attendance for a section on a date")
    @GetMapping
    public List<AttendanceDto.AttendanceRowResponse> list(
            @Parameter(description = "Section id") @RequestParam Long sectionId,
            @Parameter(description = "Date to fetch attendance for (ISO yyyy-MM-dd)") @RequestParam LocalDate date,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        requireSectionAccess(user, sectionId);
        return service.list(sectionId, date);
    }

    @Operation(summary = "Bulk mark attendance", description = "Upserts one attendance status per student for a given section + date.")
    @PostMapping("/bulk")
    public AttendanceDto.BulkAttendanceResponse bulk(
            @RequestBody @Valid AttendanceDto.BulkAttendanceRequest req,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        requireSectionAccess(user, req.sectionId());
        return service.bulkUpsert(req);
    }

    @Operation(summary = "Attendance trend", description = "Daily present-percentage for a section over the last N days.")
    @GetMapping("/trend")
    public List<AttendanceDto.DayAttendance> trend(
            @Parameter(description = "Section id") @RequestParam Long sectionId,
            @Parameter(description = "Number of trailing days to include") @RequestParam(defaultValue = "14") int days,
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
