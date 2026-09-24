package com.pm.erp.teachers.api;

import com.pm.erp.teachers.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Teachers", description = "Teacher accounts. ADMIN only.")
@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService service;

    @Operation(summary = "List all teachers")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<TeacherDto.TeacherResponse> list() {
        return service.list();
    }

    @Operation(summary = "Create a teacher", description = "Also creates the backing app_user login (username + password).")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherDto.TeacherResponse create(@RequestBody @Valid TeacherDto.TeacherRequest req) {
        return service.create(req);
    }
}
