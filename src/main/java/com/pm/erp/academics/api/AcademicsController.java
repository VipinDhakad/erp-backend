package com.pm.erp.academics.api;

import com.pm.erp.academics.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Academics", description = "Classes, sections, subjects, and academic years — the structural setup behind students, marks, and attendance")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AcademicsController {

    private final SchoolClassService classService;
    private final SectionService sectionService;
    private final SubjectService subjectService;
    private final AcademicYearService yearService;

    @Operation(summary = "List classes", description = "e.g. Grade 1, Grade 2 — the top-level grouping above sections.")
    @GetMapping("/classes")
    public List<AcademicsDto.ClassResponse> listClasses() {
        return classService.list();
    }

    @Operation(summary = "Create a class", description = "ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/classes")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicsDto.ClassResponse createClass(@RequestBody @Valid AcademicsDto.ClassRequest req) {
        return classService.create(req);
    }

    @Operation(summary = "Assign subjects to a class", description = "Replaces which subjects are taught in this class. ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/classes/{id}/subjects")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignSubjects(@PathVariable Long id, @RequestBody @Valid AcademicsDto.AssignSubjectsRequest req) {
        classService.assignSubjects(id, req);
    }

    @Operation(summary = "List sections of a class", description = "e.g. Section A, Section B for a given class + academic year.")
    @GetMapping("/sections")
    public List<AcademicsDto.SectionResponse> listSections(@Parameter(description = "Class id to filter by") @RequestParam Long classId) {
        return sectionService.listByClass(classId);
    }

    @Operation(summary = "Create a section", description = "ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicsDto.SectionResponse createSection(@RequestBody @Valid AcademicsDto.SectionRequest req) {
        return sectionService.create(req);
    }

    @Operation(summary = "List subjects")
    @GetMapping("/subjects")
    public List<AcademicsDto.SubjectResponse> listSubjects() {
        return subjectService.list();
    }

    @Operation(summary = "Create a subject", description = "ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicsDto.SubjectResponse createSubject(@RequestBody @Valid AcademicsDto.SubjectRequest req) {
        return subjectService.create(req);
    }

    @Operation(summary = "List academic years")
    @GetMapping("/academic-years")
    public List<AcademicsDto.AcademicYearResponse> listYears() {
        return yearService.list();
    }

    @Operation(summary = "Create an academic year", description = "ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/academic-years")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicsDto.AcademicYearResponse createYear(@RequestBody @Valid AcademicsDto.AcademicYearRequest req) {
        return yearService.create(req);
    }
}
