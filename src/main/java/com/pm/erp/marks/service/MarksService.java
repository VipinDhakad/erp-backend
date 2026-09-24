package com.pm.erp.marks.service;

import com.pm.erp.academics.domain.Subject;
import com.pm.erp.academics.service.SubjectService;
import com.pm.erp.common.tenant.SchoolContext;
import com.pm.erp.marks.api.MarksDto;
import com.pm.erp.marks.domain.MarkEntry;
import com.pm.erp.marks.domain.MarkEntryRepository;
import com.pm.erp.students.domain.Student;
import com.pm.erp.students.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarksService {

    private final MarkEntryRepository markRepo;
    private final StudentRepository studentRepo;
    private final SubjectService subjectService;
    private final SchoolContext schoolContext;

    @Transactional(readOnly = true)
    public List<MarksDto.MarkRowResponse> list(Long examId, Long sectionId, Long subjectId) {
        List<Student> students = studentRepo.findBySectionIdOrderByRollNoAsc(sectionId);
        List<MarkEntry> existing = markRepo.findByExamIdAndSubjectId(examId, subjectId);
        Map<Long, MarkEntry> byStudent = new HashMap<>();
        existing.forEach(m -> byStudent.put(m.getStudentId(), m));
        Subject subject = subjectService.get(subjectId);
        return students.stream().map(s -> {
            MarkEntry m = byStudent.get(s.getId());
            return new MarksDto.MarkRowResponse(
                    s.getId(),
                    s.getFirstName(),
                    s.getLastName(),
                    s.getRollNo(),
                    m == null ? null : m.getMarksObtained(),
                    subject.getMaxMarks()
            );
        }).toList();
    }

    @Transactional
    public MarksDto.BulkMarksResponse bulkUpsert(MarksDto.BulkMarksRequest req) {
        Subject subject = subjectService.get(req.subjectId());
        int maxMarks = subject.getMaxMarks();
        Long userId = schoolContext.currentUser().userId();
        int upserted = 0;
        for (var entry : req.entries()) {
            MarkEntry existing = markRepo
                    .findByStudentIdAndExamIdAndSubjectId(entry.studentId(), req.examId(), req.subjectId())
                    .orElse(null);
            BigDecimal value = clamp(entry.marksObtained(), maxMarks);
            if (existing == null) {
                MarkEntry created = MarkEntry.builder()
                        .studentId(entry.studentId())
                        .examId(req.examId())
                        .subjectId(req.subjectId())
                        .marksObtained(value)
                        .maxMarks(maxMarks)
                        .enteredByUserId(userId)
                        .enteredAt(OffsetDateTime.now())
                        .build();
                markRepo.save(created);
            } else {
                existing.setMarksObtained(value);
                existing.setMaxMarks(maxMarks);
                existing.setEnteredByUserId(userId);
                existing.setEnteredAt(OffsetDateTime.now());
            }
            upserted++;
        }
        log.info("Upserted {} mark entries examId={} sectionId={} subjectId={}",
                upserted, req.examId(), req.sectionId(), req.subjectId());
        return new MarksDto.BulkMarksResponse(upserted);
    }

    private BigDecimal clamp(BigDecimal value, int maxMarks) {
        BigDecimal max = BigDecimal.valueOf(maxMarks);
        if (value.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (value.compareTo(max) > 0) return max;
        return value;
    }
}
