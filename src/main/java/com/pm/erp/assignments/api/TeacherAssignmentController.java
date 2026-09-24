package com.pm.erp.assignments.api;

import com.pm.erp.assignments.service.TeacherAssignmentService;
import com.pm.erp.auth.security.AuthenticatedUser;
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

@Tag(name = "Teacher Assignments", description = "Which teacher teaches which section/subject — drives access control for attendance, marks, and analytics")
@RestController
@RequestMapping("/api/teacher-assignments")
@RequiredArgsConstructor
public class TeacherAssignmentController {

    private final TeacherAssignmentService service;

    /**
     * ADMIN may list everyone's assignments, or filter by any username.
     * TEACHER may only pass their own username (used by the frontend to scope
     * which sections/subjects they're allowed to teach/view).
     */
    @Operation(
            summary = "List teacher assignments",
            description = "ADMIN may list all assignments or filter by any username. TEACHER may only pass their own username."
    )
    @GetMapping
    public List<AssignmentDto.TeacherAssignmentResponse> list(
            @Parameter(description = "Filter by teacher's login username") @RequestParam(required = false) String username,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        boolean isAdmin = "ADMIN".equals(user.role());
        if (!isAdmin) {
            if (username == null || !username.equalsIgnoreCase(user.username())) {
                throw new AccessDeniedException("Teachers may only view their own assignments");
            }
            return service.listForTeacherUsername(username);
        }
        return username != null ? service.listForTeacherUsername(username) : service.list();
    }

    @Operation(summary = "Assign a teacher to a section/subject", description = "ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentDto.TeacherAssignmentResponse create(@RequestBody @Valid AssignmentDto.TeacherAssignmentRequest req) {
        return service.create(req);
    }

    @Operation(summary = "Remove a teacher assignment", description = "ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id) {
        service.remove(id);
    }
}
