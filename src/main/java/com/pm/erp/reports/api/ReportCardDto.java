package com.pm.erp.reports.api;

import com.pm.erp.marks.api.MarksDto;
import com.pm.erp.students.api.StudentDto;

import java.math.BigDecimal;
import java.util.List;

public final class ReportCardDto {

    private ReportCardDto() {}

    public record SubjectRow(
            Long subjectId,
            String subjectName,
            int maxMarks,
            BigDecimal marksObtained,
            String grade
    ) {}

    public record ReportCardResponse(
            StudentDto.StudentResponse student,
            MarksDto.ExamResponse exam,
            String className,
            String sectionName,
            String schoolName,
            String academicYear,
            List<SubjectRow> rows,
            BigDecimal totalMarks,
            int totalMaxMarks,
            double percentage,
            String overallGrade,
            double attendancePercentage,
            String remarks,
            int rankInSection,
            int sectionSize
    ) {}
}
