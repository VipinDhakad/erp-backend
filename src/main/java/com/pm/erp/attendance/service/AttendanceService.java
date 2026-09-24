package com.pm.erp.attendance.service;

import com.pm.erp.attendance.api.AttendanceDto;
import com.pm.erp.attendance.domain.AttendanceRecord;
import com.pm.erp.attendance.domain.AttendanceRecordRepository;
import com.pm.erp.common.tenant.SchoolContext;
import com.pm.erp.students.domain.Student;
import com.pm.erp.students.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRecordRepository repo;
    private final StudentRepository studentRepo;
    private final SchoolContext schoolContext;

    @Transactional(readOnly = true)
    public List<AttendanceDto.AttendanceRowResponse> list(Long sectionId, LocalDate date) {
        List<Student> students = studentRepo.findBySectionIdOrderByRollNoAsc(sectionId);
        Map<Long, AttendanceRecord> byStudent = new HashMap<>();
        repo.findBySectionIdAndDate(sectionId, date).forEach(r -> byStudent.put(r.getStudentId(), r));
        return students.stream().map(s -> {
            AttendanceRecord r = byStudent.get(s.getId());
            return new AttendanceDto.AttendanceRowResponse(
                    s.getId(), s.getFirstName(), s.getLastName(), s.getRollNo(),
                    r == null ? null : r.getStatus(),
                    r == null ? null : r.getRemark()
            );
        }).toList();
    }

    @Transactional
    public AttendanceDto.BulkAttendanceResponse bulkUpsert(AttendanceDto.BulkAttendanceRequest req) {
        Long userId = schoolContext.currentUser().userId();
        int upserted = 0;
        for (var entry : req.entries()) {
            AttendanceRecord existing = repo.findByStudentIdAndDate(entry.studentId(), req.date()).orElse(null);
            if (existing == null) {
                repo.save(AttendanceRecord.builder()
                        .studentId(entry.studentId())
                        .sectionId(req.sectionId())
                        .date(req.date())
                        .status(entry.status())
                        .remark(entry.remark())
                        .markedByUserId(userId)
                        .build());
            } else {
                existing.setStatus(entry.status());
                existing.setRemark(entry.remark());
                existing.setMarkedByUserId(userId);
            }
            upserted++;
        }
        log.info("Upserted {} attendance records sectionId={} date={}", upserted, req.sectionId(), req.date());
        return new AttendanceDto.BulkAttendanceResponse(upserted);
    }

    @Transactional(readOnly = true)
    public List<AttendanceDto.DayAttendance> sectionTrend(Long sectionId, int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Math.max(days - 1, 0));
        List<AttendanceRecord> records = repo.findBySectionIdAndDateBetweenOrderByDateAsc(sectionId, from, to);
        Map<LocalDate, List<AttendanceRecord>> byDate = new HashMap<>();
        records.forEach(r -> byDate.computeIfAbsent(r.getDate(), d -> new java.util.ArrayList<>()).add(r));
        List<AttendanceDto.DayAttendance> result = new java.util.ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            List<AttendanceRecord> dayRecords = byDate.getOrDefault(d, List.of());
            double pct = dayRecords.isEmpty() ? 0.0 :
                    100.0 * dayRecords.stream().filter(r -> r.getStatus() == com.pm.erp.attendance.domain.AttendanceStatus.PRESENT).count()
                            / dayRecords.size();
            result.add(new AttendanceDto.DayAttendance(d.toString(), Math.round(pct * 100.0) / 100.0));
        }
        return result;
    }
}
