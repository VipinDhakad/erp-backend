package com.pm.erp.analytics.api;

import java.util.List;

public final class AnalyticsDto {

    private AnalyticsDto() {}

    public record SubjectPerformance(Long subjectId, String subjectName, double percentage, String grade) {}

    public record ExamPerformance(Long examId, String examName, double percentage) {}

    public record AttendanceMonth(String month, double presentPct) {}

    public record StudentAnalyticsResponse(
            Long studentId,
            double overallPercentage,
            double attendancePercentage,
            List<SubjectPerformance> bySubject,
            List<ExamPerformance> byExam,
            List<AttendanceMonth> attendanceTrend,
            int rankInSection,
            int sectionSize
    ) {}

    public record ClassEnrolment(String className, long count) {}

    public record SubjectAverage(String subjectName, double avg) {}

    public record GenderDistribution(long male, long female, long other) {}

    public record SchoolOverviewResponse(
            List<ClassEnrolment> enrolmentByClass,
            List<SubjectAverage> avgScoreBySubject,
            List<AttendanceMonth> attendanceTrend,
            GenderDistribution genderDistribution
    ) {}
}
