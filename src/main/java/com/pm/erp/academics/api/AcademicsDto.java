package com.pm.erp.academics.api;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public final class AcademicsDto {

    private AcademicsDto() {}

    public record ClassResponse(Long id, String name, String classTeacherName, long studentCount) {}

    public record ClassRequest(
            @NotBlank @Size(max = 64) String name
    ) {}

    public record SectionResponse(Long id, Long classId, Long academicYearId, String name) {}

    public record SectionRequest(
            @NotNull Long classId,
            @NotNull Long academicYearId,
            @NotBlank @Size(max = 16) String name
    ) {}

    public record SubjectResponse(Long id, String name, String code, int maxMarks) {}

    public record SubjectRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 32) String code,
            @Min(1) @Max(1000) int maxMarks
    ) {}

    public record AcademicYearResponse(Long id, String name, LocalDate startDate, LocalDate endDate, boolean current) {}

    public record AcademicYearRequest(
            @NotBlank @Size(max = 32) String name,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            boolean current
    ) {}

    public record AssignSubjectsRequest(@NotNull List<@NotNull Long> subjectIds) {}

    public record SchoolResponse(
            Long id, String name, String code, String address, String phone,
            String logoUrl, String principalName
    ) {}

    public record SchoolRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 500) String address,
            @Size(max = 64) String phone,
            @Size(max = 500) String logoUrl,
            @Size(max = 200) String principalName
    ) {}
}
