package com.pm.erp.marks.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarkEntryRepository extends JpaRepository<MarkEntry, Long> {
    List<MarkEntry> findByStudentIdAndExamId(Long studentId, Long examId);
    List<MarkEntry> findByExamIdAndSubjectId(Long examId, Long subjectId);
    Optional<MarkEntry> findByStudentIdAndExamIdAndSubjectId(Long studentId, Long examId, Long subjectId);
    List<MarkEntry> findByStudentId(Long studentId);
    List<MarkEntry> findByStudentIdIn(List<Long> studentIds);
}
