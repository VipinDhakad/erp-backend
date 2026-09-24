package com.pm.erp.students.api;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public final class StudentDto {

    private StudentDto() {}

    public record StudentResponse(
            Long id,
            Long sectionId,
            String admissionNo,
            String firstName,
            String lastName,
            LocalDate dob,
            String gender,
            Integer rollNo
    ) {}

    public record StudentRequest(
            @NotNull Long sectionId,
            @NotBlank @Size(max = 64) String admissionNo,
            @NotBlank @Size(max = 120) String firstName,
            @NotBlank @Size(max = 120) String lastName,
            LocalDate dob,
            @Size(max = 16) String gender,
            @Min(0) @Max(500) Integer rollNo
    ) {}
}
