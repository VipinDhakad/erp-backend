package com.pm.erp.academics.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    List<AcademicYear> findBySchoolIdOrderByStartDateDesc(Long schoolId);
    Optional<AcademicYear> findBySchoolIdAndCurrentTrue(Long schoolId);
}
