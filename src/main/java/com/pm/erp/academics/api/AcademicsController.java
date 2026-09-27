package com.pm.erp.academics.api;

import com.pm.erp.academics.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Academics", description = "Classes, sections, subjects, and academic years — the structural setup behind students, marks, and attendance")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AcademicsController {

    private static final String CLASS_EXAMPLE = """
            {
              "id": 3,
              "name": "Class 8",
              "displayOrder": 8
            }
            """;
    private static final String SECTION_EXAMPLE = """
            {
              "id": 5,
              "classId": 3,
              "academicYearId": 1,
              "name": "A"
            }
            """;
    private static final String SUBJECT_EXAMPLE = """
            {
              "id": 3,
              "name": "Mathematics",
              "code": "MAT",
              "maxMarks": 100
            }
            """;
    private static final String YEAR_EXAMPLE = """
            {
              "id": 1,
              "name": "2026-27",
              "startDate": "2026-04-01",
              "endDate": "2027-03-31",
              "current": true
            }
            """;

    private final SchoolClassService classService;
    private final SectionService sectionService;
    private final SubjectService subjectService;
    private final AcademicYearService yearService;

    @Operation(summary = "List classes", description = "e.g. Grade 1, Grade 2 — the top-level grouping above sections.")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
            value = """
                    [
                      { "id": 1, "name": "Class 6", "displayOrder": 6 },
                      { "id": 2, "name": "Class 7", "displayOrder": 7 },
                      { "id": 3, "name": "Class 8", "displayOrder": 8 }
                    ]
                    """
    )))
    @GetMapping("/classes")
    public List<AcademicsDto.ClassResponse> listClasses() {
        return classService.list();
    }

    @Operation(summary = "Create a class", description = "ADMIN only.")
    @ApiResponse(responseCode = "201", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = CLASS_EXAMPLE)))
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
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
            value = """
                    [
                      { "id": 5, "classId": 3, "academicYearId": 1, "name": "A" },
                      { "id": 6, "classId": 3, "academicYearId": 1, "name": "B" }
                    ]
                    """
    )))
    @GetMapping("/sections")
    public List<AcademicsDto.SectionResponse> listSections(@Parameter(description = "Class id to filter by") @RequestParam Long classId) {
        return sectionService.listByClass(classId);
    }

    @Operation(summary = "Create a section", description = "ADMIN only.")
    @ApiResponse(responseCode = "201", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = SECTION_EXAMPLE)))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicsDto.SectionResponse createSection(@RequestBody @Valid AcademicsDto.SectionRequest req) {
        return sectionService.create(req);
    }

    @Operation(summary = "List subjects")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
            value = """
                    [
                      { "id": 1, "name": "English", "code": "ENG", "maxMarks": 100 },
                      { "id": 3, "name": "Mathematics", "code": "MAT", "maxMarks": 100 }
                    ]
                    """
    )))
    @GetMapping("/subjects")
    public List<AcademicsDto.SubjectResponse> listSubjects() {
        return subjectService.list();
    }

    @Operation(summary = "Create a subject", description = "ADMIN only.")
    @ApiResponse(responseCode = "201", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = SUBJECT_EXAMPLE)))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicsDto.SubjectResponse createSubject(@RequestBody @Valid AcademicsDto.SubjectRequest req) {
        return subjectService.create(req);
    }

    @Operation(summary = "List academic years")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
            value = "[" + YEAR_EXAMPLE + "]"
    )))
    @GetMapping("/academic-years")
    public List<AcademicsDto.AcademicYearResponse> listYears() {
        return yearService.list();
    }

    @Operation(summary = "Create an academic year", description = "ADMIN only.")
    @ApiResponse(responseCode = "201", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = YEAR_EXAMPLE)))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/academic-years")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicsDto.AcademicYearResponse createYear(@RequestBody @Valid AcademicsDto.AcademicYearRequest req) {
        return yearService.create(req);
    }
}
