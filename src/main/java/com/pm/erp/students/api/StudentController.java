package com.pm.erp.students.api;

import com.pm.erp.students.service.StudentService;
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

@Tag(name = "Students", description = "Student roster management, scoped by section")
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private static final String STUDENT_EXAMPLE = """
            {
              "id": 1,
              "sectionId": 5,
              "admissionNo": "A-8A-001",
              "firstName": "Aarav",
              "lastName": "Sharma",
              "dob": "2011-04-15",
              "gender": "M",
              "rollNo": 1
            }
            """;

    private final StudentService service;

    @Operation(summary = "List students in a section")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
            value = """
                    [
                      {
                        "id": 1,
                        "sectionId": 5,
                        "admissionNo": "A-8A-001",
                        "firstName": "Aarav",
                        "lastName": "Sharma",
                        "dob": "2011-04-15",
                        "gender": "M",
                        "rollNo": 1
                      },
                      {
                        "id": 2,
                        "sectionId": 5,
                        "admissionNo": "A-8A-002",
                        "firstName": "Diya",
                        "lastName": "Patel",
                        "dob": "2011-06-21",
                        "gender": "F",
                        "rollNo": 2
                      }
                    ]
                    """
    )))
    @GetMapping
    public List<StudentDto.StudentResponse> list(@Parameter(description = "Section id to filter by") @RequestParam Long sectionId) {
        return service.listBySection(sectionId);
    }

    @Operation(summary = "Get a student by id")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = STUDENT_EXAMPLE)))
    @GetMapping("/{id}")
    public StudentDto.StudentResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @Operation(summary = "Create a student", description = "ADMIN only.")
    @ApiResponse(responseCode = "201", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = STUDENT_EXAMPLE)))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentDto.StudentResponse create(@RequestBody @Valid StudentDto.StudentRequest req) {
        return service.create(req);
    }

    @Operation(summary = "Update a student", description = "ADMIN only.")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = STUDENT_EXAMPLE)))
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
