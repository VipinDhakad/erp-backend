package com.pm.erp.attendance.api;

import com.pm.erp.attendance.domain.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public final class AttendanceDto {

    private AttendanceDto() {}

    public record AttendanceRowResponse(
            Long studentId,
            String firstName,
            String lastName,
            Integer rollNo,
            AttendanceStatus status,
            String remark
    ) {}

    public record BulkAttendanceRequest(
            @NotNull Long sectionId,
            @NotNull LocalDate date,
            @NotNull @Size(min = 1) List<@Valid Entry> entries
    ) {
        public record Entry(
                @NotNull Long studentId,
                @NotNull AttendanceStatus status,
                String remark
        ) {}
    }

    public record BulkAttendanceResponse(int upserted) {}

    public record DayAttendance(String date, double presentPct) {}
}
