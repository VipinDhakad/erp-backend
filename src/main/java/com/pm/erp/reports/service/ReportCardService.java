package com.pm.erp.reports.service;

import com.lowagie.text.DocumentException;
import com.pm.erp.academics.domain.*;
import com.pm.erp.academics.service.AcademicYearService;
import com.pm.erp.academics.service.SectionService;
import com.pm.erp.academics.service.SubjectService;
import com.pm.erp.attendance.domain.AttendanceRecord;
import com.pm.erp.attendance.domain.AttendanceRecordRepository;
import com.pm.erp.attendance.domain.AttendanceStatus;
import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.marks.api.MarksDto;
import com.pm.erp.marks.domain.Exam;
import com.pm.erp.marks.domain.MarkEntry;
import com.pm.erp.marks.domain.MarkEntryRepository;
import com.pm.erp.marks.service.ExamService;
import com.pm.erp.reports.api.ReportCardDto;
import com.pm.erp.students.api.StudentDto;
import com.pm.erp.students.domain.Student;
import com.pm.erp.students.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Renders a single-student, single-exam report card as a PDF.
 * Pipeline: Thymeleaf HTML template -> Flying Saucer (OpenPDF) -> bytes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportCardService {

    private final TemplateEngine templateEngine;
    private final StudentRepository studentRepo;
    private final ExamService examService;
    private final MarkEntryRepository markRepo;
    private final SubjectService subjectService;
    private final SectionService sectionService;
    private final SchoolRepository schoolRepo;
    private final SchoolClassRepository classRepo;
    private final AcademicYearService academicYearService;
    private final AttendanceRecordRepository attendanceRepo;

    @Transactional(readOnly = true)
    public byte[] render(Long studentId, Long examId) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));
        Exam exam = examService.get(examId);
        Section section = sectionService.get(student.getSectionId());
        SchoolClass schoolClass = classRepo.findById(section.getClassId())
                .orElseThrow(() -> new NotFoundException("Class not found: " + section.getClassId()));
        School school = schoolRepo.findById(student.getSchoolId())
                .orElseThrow(() -> new NotFoundException("School not found: " + student.getSchoolId()));

        List<MarkEntry> entries = markRepo.findByStudentIdAndExamId(studentId, examId);
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal totalObtained = BigDecimal.ZERO;
        int totalMax = 0;
        for (MarkEntry m : entries) {
            Subject subject = subjectService.get(m.getSubjectId());
            BigDecimal pct = m.getMarksObtained()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(m.getMaxMarks()), 2, RoundingMode.HALF_UP);
            rows.add(Map.of(
                    "subjectName", subject.getName(),
                    "subjectCode", subject.getCode(),
                    "maxMarks", m.getMaxMarks(),
                    "marksObtained", m.getMarksObtained(),
                    "percentage", pct
            ));
            totalObtained = totalObtained.add(m.getMarksObtained());
            totalMax += m.getMaxMarks();
        }
        BigDecimal overallPct = totalMax == 0 ? BigDecimal.ZERO :
                totalObtained.multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalMax), 2, RoundingMode.HALF_UP);

        Context ctx = new Context();
        ctx.setVariable("school", Map.of(
                "name", school.getName(),
                "address", Optional.ofNullable(school.getAddress()).orElse(""),
                "phone", Optional.ofNullable(school.getPhone()).orElse(""),
                "principalName", Optional.ofNullable(school.getPrincipalName()).orElse("Principal")
        ));
        ctx.setVariable("student", Map.of(
                "firstName", student.getFirstName(),
                "lastName", student.getLastName(),
                "admissionNo", student.getAdmissionNo(),
                "rollNo", Optional.ofNullable(student.getRollNo()).map(String::valueOf).orElse("—"),
                "dob", Optional.ofNullable(student.getDob()).map(LocalDate::toString).orElse("—"),
                "gender", Optional.ofNullable(student.getGender()).orElse("—"),
                "className", schoolClass.getName(),
                "sectionName", section.getName()
        ));
        ctx.setVariable("exam", Map.of(
                "name", exam.getName(),
                "academicYearName", academicYearService.get(exam.getAcademicYearId()).getName()
        ));
        ctx.setVariable("rows", rows);
        ctx.setVariable("totalObtained", totalObtained);
        ctx.setVariable("totalMax", totalMax);
        ctx.setVariable("overallPercentage", overallPct);
        ctx.setVariable("grade", grade(overallPct));
        ctx.setVariable("issuedOn", LocalDate.now().toString());

        String html = templateEngine.process("reports/report-card", ctx);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            log.info("Rendered report card studentId={} examId={} bytes={}", studentId, examId, out.size());
            return out.toByteArray();
        } catch (DocumentException | IOException ex) {
            log.error("Failed to render report card studentId={} examId={}", studentId, examId, ex);
            throw new IllegalStateException("Failed to render report card", ex);
        }
    }

    @Transactional(readOnly = true)
    public ReportCardDto.ReportCardResponse buildReportCard(Long studentId, Long examId) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));
        Exam exam = examService.get(examId);
        Section section = sectionService.get(student.getSectionId());
        SchoolClass schoolClass = classRepo.findById(section.getClassId())
                .orElseThrow(() -> new NotFoundException("Class not found: " + section.getClassId()));
        School school = schoolRepo.findById(student.getSchoolId())
                .orElseThrow(() -> new NotFoundException("School not found: " + student.getSchoolId()));

        List<MarkEntry> entries = markRepo.findByStudentIdAndExamId(studentId, examId);
        List<ReportCardDto.SubjectRow> rows = new ArrayList<>();
        BigDecimal totalObtained = BigDecimal.ZERO;
        int totalMax = 0;
        for (MarkEntry m : entries) {
            Subject subject = subjectService.get(m.getSubjectId());
            rows.add(new ReportCardDto.SubjectRow(
                    subject.getId(), subject.getName(), m.getMaxMarks(), m.getMarksObtained(),
                    grade(percentageOf(m.getMarksObtained(), m.getMaxMarks()))));
            totalObtained = totalObtained.add(m.getMarksObtained());
            totalMax += m.getMaxMarks();
        }
        BigDecimal overallPct = percentageOf(totalObtained, totalMax);

        List<AttendanceRecord> attendance = attendanceRepo.findByStudentId(studentId);
        double attendancePct = attendance.isEmpty() ? 0.0 :
                Math.round(100.0 * attendance.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count()
                        / attendance.size() * 100.0) / 100.0;

        List<Student> sectionStudents = studentRepo.findBySectionIdOrderByRollNoAsc(section.getId());
        Map<Long, BigDecimal> pctByStudent = new HashMap<>();
        for (Student s : sectionStudents) {
            List<MarkEntry> sEntries = markRepo.findByStudentIdAndExamId(s.getId(), examId);
            BigDecimal obtained = sEntries.stream().map(MarkEntry::getMarksObtained).reduce(BigDecimal.ZERO, BigDecimal::add);
            int max = sEntries.stream().mapToInt(MarkEntry::getMaxMarks).sum();
            pctByStudent.put(s.getId(), percentageOf(obtained, max));
        }
        List<Long> ranked = pctByStudent.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
        int rank = ranked.indexOf(studentId) + 1;

        return new ReportCardDto.ReportCardResponse(
                new StudentDto.StudentResponse(student.getId(), student.getSectionId(), student.getAdmissionNo(),
                        student.getFirstName(), student.getLastName(), student.getDob(), student.getGender(), student.getRollNo()),
                new MarksDto.ExamResponse(exam.getId(), exam.getName(), exam.getStartDate(), exam.getEndDate(), exam.getAcademicYearId()),
                schoolClass.getName(),
                section.getName(),
                school.getName(),
                academicYearService.get(exam.getAcademicYearId()).getName(),
                rows,
                totalObtained,
                totalMax,
                overallPct.doubleValue(),
                grade(overallPct),
                attendancePct,
                "",
                rank,
                sectionStudents.size()
        );
    }

    private BigDecimal percentageOf(BigDecimal obtained, int max) {
        if (max == 0) return BigDecimal.ZERO;
        return obtained.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(max), 2, RoundingMode.HALF_UP);
    }

    private String grade(BigDecimal pct) {
        double p = pct.doubleValue();
        if (p >= 91) return "A1";
        if (p >= 81) return "A2";
        if (p >= 71) return "B1";
        if (p >= 61) return "B2";
        if (p >= 51) return "C1";
        if (p >= 41) return "C2";
        if (p >= 33) return "D";
        return "E";
    }
}
