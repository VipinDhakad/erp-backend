package com.pm.erp.students.api;

import com.pm.erp.assignments.service.TeacherAssignmentService;
import com.pm.erp.auth.security.AuthenticatedUser;
import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.students.service.StudentService;
import com.pm.erp.teachers.domain.TeacherRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final TeacherAssignmentService assignmentService;
    private final TeacherRepository teacherRepo;

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

    @Operation(
            summary = "Update a student",
            description = "ADMIN may edit any student. TEACHER may edit a student only if they are the class teacher " +
                    "of that student's current section, and may not move the student to a different section."
    )
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = STUDENT_EXAMPLE)))
    @PutMapping("/{id}")
    public StudentDto.StudentResponse update(
            @PathVariable Long id,
            @RequestBody @Valid StudentDto.StudentRequest req,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        requireEditAccess(user, id, req.sectionId());
        return service.update(id, req);
    }

    @Operation(summary = "Delete a student", description = "ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    private void requireEditAccess(AuthenticatedUser user, Long studentId, Long requestedSectionId) {
        if ("ADMIN".equals(user.role())) {
            return;
        }
        Long currentSectionId = service.get(studentId).sectionId();
        if (!requestedSectionId.equals(currentSectionId)) {
            throw new AccessDeniedException("Teachers may not move a student to a different section");
        }
        Long teacherId = teacherRepo.findByUserId(user.userId())
                .orElseThrow(() -> new NotFoundException("Teacher not found for user: " + user.username()))
                .getId();
        boolean isClassTeacher = assignmentService.classTeacherSectionIds(teacherId).contains(currentSectionId);
        if (!isClassTeacher) {
            throw new AccessDeniedException("Only the class teacher of this section may edit this student");
        }
    }
}
