package com.pm.erp.academics.api;

import com.pm.erp.academics.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AcademicsController {

    private final SchoolClassService classService;
    private final SectionService sectionService;
    private final SubjectService subjectService;
    private final AcademicYearService yearService;

    @GetMapping("/classes")
    public List<AcademicsDto.ClassResponse> listClasses() {
        return classService.list();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/classes")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicsDto.ClassResponse createClass(@RequestBody @Valid AcademicsDto.ClassRequest req) {
        return classService.create(req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/classes/{id}/subjects")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignSubjects(@PathVariable Long id, @RequestBody @Valid AcademicsDto.AssignSubjectsRequest req) {
        classService.assignSubjects(id, req);
    }

    @GetMapping("/sections")
    public List<AcademicsDto.SectionResponse> listSections(@RequestParam Long classId) {
        return sectionService.listByClass(classId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicsDto.SectionResponse createSection(@RequestBody @Valid AcademicsDto.SectionRequest req) {
        return sectionService.create(req);
    }

    @GetMapping("/subjects")
    public List<AcademicsDto.SubjectResponse> listSubjects() {
        return subjectService.list();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicsDto.SubjectResponse createSubject(@RequestBody @Valid AcademicsDto.SubjectRequest req) {
        return subjectService.create(req);
    }

    @GetMapping("/academic-years")
    public List<AcademicsDto.AcademicYearResponse> listYears() {
        return yearService.list();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/academic-years")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicsDto.AcademicYearResponse createYear(@RequestBody @Valid AcademicsDto.AcademicYearRequest req) {
        return yearService.create(req);
    }
}
