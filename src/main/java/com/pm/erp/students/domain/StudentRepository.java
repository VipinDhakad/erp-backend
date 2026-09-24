package com.pm.erp.students.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findBySectionIdOrderByRollNoAsc(Long sectionId);
    List<Student> findBySchoolIdOrderByLastNameAscFirstNameAsc(Long schoolId);
    boolean existsBySchoolIdAndAdmissionNoIgnoreCase(Long schoolId, String admissionNo);
}
