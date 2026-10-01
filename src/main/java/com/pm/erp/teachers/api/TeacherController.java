package com.pm.erp.teachers.api;

import com.pm.erp.teachers.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "Teachers", description = "Teacher accounts. ADMIN only.")
@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private static final String TEACHER_EXAMPLE = """
            {
              "id": 1,
              "firstName": "Meera",
              "lastName": "Sharma",
              "employeeNo": "EMP-101",
              "username": "teacher1"
            }
            """;

    private final TeacherService service;

    @Operation(summary = "List all teachers")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
            value = """
                    [
                      {
                        "id": 1,
                        "firstName": "Meera",
                        "lastName": "Sharma",
                        "employeeNo": "EMP-101",
                        "username": "teacher1"
                      },
                      {
                        "id": 2,
                        "firstName": "Rajesh",
                        "lastName": "Kulkarni",
                        "employeeNo": "EMP-102",
                        "username": "teacher2"
                      }
                    ]
                    """
    )))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<TeacherDto.TeacherResponse> list() {
        return service.list();
    }

    @Operation(summary = "Create a teacher", description = "Also creates the backing app_user login (username + password).")
    @ApiResponse(responseCode = "201", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = TEACHER_EXAMPLE)))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherDto.TeacherResponse create(@RequestBody @Valid TeacherDto.TeacherRequest req) {
        return service.create(req);
    }

    @Operation(summary = "Update a teacher", description = "Updates name/employee number only — does not change username or password.")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = TEACHER_EXAMPLE)))
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public TeacherDto.TeacherResponse update(@PathVariable Long id, @RequestBody @Valid TeacherDto.TeacherUpdateRequest req) {
        return service.update(id, req);
    }

    @Operation(summary = "Delete a teacher", description = "ADMIN only. Fails if the teacher still has active class/section assignments — unassign them first. Disables (does not delete) the backing login.")
    @ApiResponse(responseCode = "204")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
