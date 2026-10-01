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
              "classTeacherName": "Meera Sharma",
              "studentCount": 25
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
    private static final String SCHOOL_EXAMPLE = """
            {
              "id": 1,
              "name": "Sunrise Public School",
              "code": "SPS-001",
              "address": "M.G. Road, Pune, Maharashtra 411001",
              "phone": "+91-20-1234-5678",
              "logoUrl": null,
              "principalName": "Mrs. Sunita Verma"
            }
            """;

    private final SchoolClassService classService;
    private final SectionService sectionService;
    private final SubjectService subjectService;
    private final AcademicYearService yearService;
    private final SchoolService schoolService;

    @Operation(summary = "List classes", description = "e.g. Grade 1, Grade 2 — the top-level grouping above sections. Each entry includes its class teacher's name (if any section in the class has one assigned) and total student count across all its sections.")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
            value = """
                    [
                      { "id": 1, "name": "Class 6", "classTeacherName": null, "studentCount": 40 },
                      { "id": 2, "name": "Class 7", "classTeacherName": "Rajesh Kulkarni", "studentCount": 38 },
                      { "id": 3, "name": "Class 8", "classTeacherName": "Meera Sharma", "studentCount": 50 }
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

    @Operation(summary = "Update a class", description = "Rename an existing class. ADMIN only.")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = CLASS_EXAMPLE)))
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/classes/{id}")
    public AcademicsDto.ClassResponse updateClass(@PathVariable Long id, @RequestBody @Valid AcademicsDto.ClassUpdateRequest req) {
        return classService.update(id, req);
    }

    @Operation(summary = "Delete a class", description = "ADMIN only. Fails if the class still has sections — remove those first.")
    @ApiResponse(responseCode = "204")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/classes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClass(@PathVariable Long id) {
        classService.delete(id);
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

    @Operation(summary = "List subjects", description = "Optionally filter to subjects assigned to a specific class.")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
            value = """
                    [
                      { "id": 1, "name": "English", "code": "ENG", "maxMarks": 100 },
                      { "id": 3, "name": "Mathematics", "code": "MAT", "maxMarks": 100 }
                    ]
                    """
    )))
    @GetMapping("/subjects")
    public List<AcademicsDto.SubjectResponse> listSubjects(
            @Parameter(description = "Filter to subjects assigned to this class only") @RequestParam(required = false) Long classId
    ) {
        return subjectService.list(classId);
    }

    @Operation(summary = "Create a subject", description = "ADMIN only.")
    @ApiResponse(responseCode = "201", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = SUBJECT_EXAMPLE)))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicsDto.SubjectResponse createSubject(@RequestBody @Valid AcademicsDto.SubjectRequest req) {
        return subjectService.create(req);
    }

    @Operation(summary = "Update a subject", description = "ADMIN only.")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = SUBJECT_EXAMPLE)))
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/subjects/{id}")
    public AcademicsDto.SubjectResponse updateSubject(@PathVariable Long id, @RequestBody @Valid AcademicsDto.SubjectUpdateRequest req) {
        return subjectService.update(id, req);
    }

    @Operation(summary = "Delete a subject", description = "ADMIN only. Fails if the subject is still assigned to a class.")
    @ApiResponse(responseCode = "204")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/subjects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubject(@PathVariable Long id) {
        subjectService.delete(id);
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

    @Operation(summary = "Update an academic year", description = "ADMIN only.")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = YEAR_EXAMPLE)))
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/academic-years/{id}")
    public AcademicsDto.AcademicYearResponse updateYear(@PathVariable Long id, @RequestBody @Valid AcademicsDto.AcademicYearUpdateRequest req) {
        return yearService.update(id, req);
    }

    @Operation(summary = "Delete an academic year", description = "ADMIN only. Fails if the academic year still has sections.")
    @ApiResponse(responseCode = "204")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/academic-years/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteYear(@PathVariable Long id) {
        yearService.delete(id);
    }

    @Operation(summary = "Get school details", description = "The caller's own school (tenant) — name, address, phone, principal, logo.")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = SCHOOL_EXAMPLE)))
    @GetMapping("/school")
    public AcademicsDto.SchoolResponse getSchool() {
        return schoolService.get();
    }

    @Operation(summary = "Update school details", description = "ADMIN only. The school's unique code cannot be changed via this endpoint.")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = SCHOOL_EXAMPLE)))
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/school")
    public AcademicsDto.SchoolResponse updateSchool(@RequestBody @Valid AcademicsDto.SchoolRequest req) {
        return schoolService.update(req);
    }
}
