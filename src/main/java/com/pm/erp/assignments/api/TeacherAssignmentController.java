package com.pm.erp.assignments.api;

import com.pm.erp.assignments.service.TeacherAssignmentService;
import com.pm.erp.auth.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @GetMapping
    public List<AssignmentDto.TeacherAssignmentResponse> list(
            @RequestParam(required = false) String username,
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

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentDto.TeacherAssignmentResponse create(@RequestBody @Valid AssignmentDto.TeacherAssignmentRequest req) {
        return service.create(req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id) {
        service.remove(id);
    }
}
