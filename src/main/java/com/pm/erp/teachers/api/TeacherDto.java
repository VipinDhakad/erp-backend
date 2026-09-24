package com.pm.erp.teachers.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class TeacherDto {

    private TeacherDto() {}

    public record TeacherResponse(
            Long id,
            String firstName,
            String lastName,
            String employeeNo,
            String username
    ) {}

    public record TeacherRequest(
            @NotBlank @Size(max = 120) String firstName,
            @NotBlank @Size(max = 120) String lastName,
            @Size(max = 64) String employeeNo,
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(min = 6, max = 128) String password
    ) {}
}
