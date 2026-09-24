package com.pm.erp.assignments.api;

import jakarta.validation.constraints.NotNull;

public final class AssignmentDto {

    private AssignmentDto() {}

    public record TeacherAssignmentResponse(
            Long id,
            Long teacherId,
            Long sectionId,
            Long subjectId,
            boolean isClassTeacher
    ) {}

    public record TeacherAssignmentRequest(
            @NotNull Long teacherId,
            @NotNull Long sectionId,
            Long subjectId,
            boolean isClassTeacher
    ) {}
}
