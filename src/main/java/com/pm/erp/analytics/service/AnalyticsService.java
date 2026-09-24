package com.pm.erp.analytics.service;

import com.pm.erp.academics.domain.*;
import com.pm.erp.analytics.api.AnalyticsDto;
import com.pm.erp.attendance.domain.AttendanceRecord;
import com.pm.erp.attendance.domain.AttendanceRecordRepository;
import com.pm.erp.attendance.domain.AttendanceStatus;
import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.common.tenant.SchoolContext;
import com.pm.erp.marks.domain.Exam;
import com.pm.erp.marks.domain.ExamRepository;
import com.pm.erp.marks.domain.MarkEntry;
import com.pm.erp.marks.domain.MarkEntryRepository;
import com.pm.erp.students.domain.Student;
import com.pm.erp.students.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final StudentRepository studentRepo;
    private final SectionRepository sectionRepo;
    private final SchoolClassRepository classRepo;
    private final SubjectRepository subjectRepo;
    private final ExamRepository examRepo;
    private final MarkEntryRepository markRepo;
    private final AttendanceRecordRepository attendanceRepo;
    private final SchoolContext schoolContext;

    @Transactional(readOnly = true)
    public AnalyticsDto.StudentAnalyticsResponse forStudent(Long studentId) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));

        List<MarkEntry> marks = markRepo.findByStudentId(studentId);
        Map<Long, Subject> subjectsById = subjectRepo.findAllById(
                marks.stream().map(MarkEntry::getSubjectId).distinct().toList()
        ).stream().collect(Collectors.toMap(Subject::getId, s -> s));
        Map<Long, Exam> examsById = examRepo.findAllById(
                marks.stream().map(MarkEntry::getExamId).distinct().toList()
        ).stream().collect(Collectors.toMap(Exam::getId, e -> e));

        List<AnalyticsDto.SubjectPerformance> bySubject = marks.stream()
                .collect(Collectors.groupingBy(MarkEntry::getSubjectId))
                .entrySet().stream()
                .map(e -> {
                    double pct = percentage(e.getValue());
                    Subject subject = subjectsById.get(e.getKey());
                    return new AnalyticsDto.SubjectPerformance(
                            e.getKey(), subject == null ? "Unknown" : subject.getName(), pct, grade(pct));
                })
                .sorted(Comparator.comparing(AnalyticsDto.SubjectPerformance::subjectName))
                .toList();

        List<AnalyticsDto.ExamPerformance> byExam = marks.stream()
                .collect(Collectors.groupingBy(MarkEntry::getExamId))
                .entrySet().stream()
                .map(e -> {
                    Exam exam = examsById.get(e.getKey());
                    return new AnalyticsDto.ExamPerformance(
                            e.getKey(), exam == null ? "Unknown" : exam.getName(), percentage(e.getValue()));
                })
                .sorted(Comparator.comparing(AnalyticsDto.ExamPerformance::examId))
                .toList();

        double overallPct = percentage(marks);

        List<AttendanceRecord> attendance = attendanceRepo.findByStudentId(studentId);
        double attendancePct = attendancePercentage(attendance);
        List<AnalyticsDto.AttendanceMonth> attendanceTrend = monthlyAttendanceTrend(attendance);

        List<Student> sectionStudents = studentRepo.findBySectionIdOrderByRollNoAsc(student.getSectionId());
        Map<Long, Double> pctByStudent = sectionStudents.stream().collect(Collectors.toMap(
                Student::getId,
                s -> s.getId().equals(studentId) ? overallPct : percentage(markRepo.findByStudentId(s.getId()))
        ));
        List<Long> ranked = pctByStudent.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
        int rank = ranked.indexOf(studentId) + 1;

        return new AnalyticsDto.StudentAnalyticsResponse(
                studentId, overallPct, attendancePct, bySubject, byExam, attendanceTrend,
                rank, sectionStudents.size());
    }

    @Transactional(readOnly = true)
    public AnalyticsDto.SchoolOverviewResponse schoolOverview() {
        Long schoolId = schoolContext.currentSchoolId();

        List<SchoolClass> classes = classRepo.findBySchoolIdOrderByDisplayOrderAsc(schoolId);
        List<AnalyticsDto.ClassEnrolment> enrolmentByClass = classes.stream().map(c -> {
            List<Section> sections = sectionRepo.findByClassIdOrderByNameAsc(c.getId());
            long count = sections.stream()
                    .mapToLong(s -> studentRepo.findBySectionIdOrderByRollNoAsc(s.getId()).size())
                    .sum();
            return new AnalyticsDto.ClassEnrolment(c.getName(), count);
        }).toList();

        List<Student> allStudents = studentRepo.findBySchoolIdOrderByLastNameAscFirstNameAsc(schoolId);
        List<Subject> subjects = subjectRepo.findBySchoolIdOrderByNameAsc(schoolId);
        List<MarkEntry> allMarks = markRepo.findByStudentIdIn(allStudents.stream().map(Student::getId).toList());
        Map<Long, Subject> subjectsById = subjects.stream().collect(Collectors.toMap(Subject::getId, s -> s));
        List<AnalyticsDto.SubjectAverage> avgScoreBySubject = allMarks.stream()
                .collect(Collectors.groupingBy(MarkEntry::getSubjectId))
                .entrySet().stream()
                .map(e -> new AnalyticsDto.SubjectAverage(
                        Optional.ofNullable(subjectsById.get(e.getKey())).map(Subject::getName).orElse("Unknown"),
                        percentage(e.getValue())))
                .sorted(Comparator.comparing(AnalyticsDto.SubjectAverage::subjectName))
                .toList();

        List<Long> sectionIds = classes.stream()
                .flatMap(c -> sectionRepo.findByClassIdOrderByNameAsc(c.getId()).stream())
                .map(Section::getId)
                .toList();
        List<AttendanceRecord> allAttendance = attendanceRepo.findBySectionIdIn(sectionIds);
        List<AnalyticsDto.AttendanceMonth> attendanceTrend = monthlyAttendanceTrend(allAttendance);

        long male = allStudents.stream().filter(s -> "M".equalsIgnoreCase(s.getGender())).count();
        long female = allStudents.stream().filter(s -> "F".equalsIgnoreCase(s.getGender())).count();
        long other = allStudents.size() - male - female;

        return new AnalyticsDto.SchoolOverviewResponse(
                enrolmentByClass, avgScoreBySubject, attendanceTrend,
                new AnalyticsDto.GenderDistribution(male, female, other));
    }

    private double percentage(List<MarkEntry> marks) {
        if (marks.isEmpty()) return 0.0;
        BigDecimal obtained = marks.stream().map(MarkEntry::getMarksObtained).reduce(BigDecimal.ZERO, BigDecimal::add);
        int max = marks.stream().mapToInt(MarkEntry::getMaxMarks).sum();
        if (max == 0) return 0.0;
        return obtained.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(max), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double attendancePercentage(List<AttendanceRecord> records) {
        if (records.isEmpty()) return 0.0;
        long present = records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
        return Math.round(100.0 * present / records.size() * 100.0) / 100.0;
    }

    private List<AnalyticsDto.AttendanceMonth> monthlyAttendanceTrend(List<AttendanceRecord> records) {
        Map<YearMonth, List<AttendanceRecord>> byMonth = records.stream()
                .collect(Collectors.groupingBy(r -> YearMonth.from(r.getDate())));
        return byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new AnalyticsDto.AttendanceMonth(
                        e.getKey().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + e.getKey().getYear(),
                        attendancePercentage(e.getValue())))
                .toList();
    }

    private String grade(double pct) {
        if (pct >= 91) return "A1";
        if (pct >= 81) return "A2";
        if (pct >= 71) return "B1";
        if (pct >= 61) return "B2";
        if (pct >= 51) return "C1";
        if (pct >= 41) return "C2";
        if (pct >= 33) return "D";
        return "E";
    }
}
