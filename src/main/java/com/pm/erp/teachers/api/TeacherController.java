package com.pm.erp.teachers.api;

import com.pm.erp.teachers.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService service;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<TeacherDto.TeacherResponse> list() {
        return service.list();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherDto.TeacherResponse create(@RequestBody @Valid TeacherDto.TeacherRequest req) {
        return service.create(req);
    }
}
