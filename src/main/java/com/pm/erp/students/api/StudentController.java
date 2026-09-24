package com.pm.erp.students.api;

import com.pm.erp.students.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Students", description = "Student roster management, scoped by section")
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService service;

    @Operation(summary = "List students in a section")
    @GetMapping
    public List<StudentDto.StudentResponse> list(@Parameter(description = "Section id to filter by") @RequestParam Long sectionId) {
        return service.listBySection(sectionId);
    }

    @Operation(summary = "Get a student by id")
    @GetMapping("/{id}")
    public StudentDto.StudentResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @Operation(summary = "Create a student", description = "ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentDto.StudentResponse create(@RequestBody @Valid StudentDto.StudentRequest req) {
        return service.create(req);
    }

    @Operation(summary = "Update a student", description = "ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public StudentDto.StudentResponse update(@PathVariable Long id, @RequestBody @Valid StudentDto.StudentRequest req) {
        return service.update(id, req);
    }

    @Operation(summary = "Delete a student", description = "ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
