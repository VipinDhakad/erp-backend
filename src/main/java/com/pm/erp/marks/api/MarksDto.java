package com.pm.erp.marks.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class MarksDto {

    private MarksDto() {}

    public record ExamResponse(Long id, String name, LocalDate startDate, LocalDate endDate, Long academicYearId) {}

    public record ExamRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull Long academicYearId,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record MarkRowResponse(
            Long studentId,
            String firstName,
            String lastName,
            Integer rollNo,
            BigDecimal marksObtained,
            int maxMarks
    ) {}

    public record BulkMarksRequest(
            @NotNull Long examId,
            @NotNull Long sectionId,
            @NotNull Long subjectId,
            @NotNull @Size(min = 1) List<@Valid Entry> entries
    ) {
        public record Entry(
                @NotNull Long studentId,
                @NotNull @DecimalMin("0.0") @DecimalMax("1000.0") BigDecimal marksObtained
        ) {}
    }

    public record BulkMarksResponse(int upserted) {}
}
