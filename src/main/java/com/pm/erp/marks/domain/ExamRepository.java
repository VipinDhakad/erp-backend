package com.pm.erp.marks.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findBySchoolIdOrderByStartDateDescIdDesc(Long schoolId);
    List<Exam> findBySchoolIdAndClassIdOrderByStartDateDescIdDesc(Long schoolId, Long classId);
}
