package com.pm.erp.attendance.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findBySectionIdAndDate(Long sectionId, LocalDate date);
    List<AttendanceRecord> findBySectionIdAndDateBetweenOrderByDateAsc(Long sectionId, LocalDate from, LocalDate to);
    Optional<AttendanceRecord> findByStudentIdAndDate(Long studentId, LocalDate date);
    List<AttendanceRecord> findByStudentId(Long studentId);
    List<AttendanceRecord> findBySectionIdIn(List<Long> sectionIds);
}
