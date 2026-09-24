package com.pm.erp.assignments.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {
    List<TeacherAssignment> findByTeacherId(Long teacherId);
    List<TeacherAssignment> findAllByOrderBySectionIdAsc();
    Optional<TeacherAssignment> findByTeacherIdAndSectionIdAndSubjectId(Long teacherId, Long sectionId, Long subjectId);
    boolean existsByTeacherIdAndSectionIdAndSubjectId(Long teacherId, Long sectionId, Long subjectId);
}
