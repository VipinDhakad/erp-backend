package com.pm.erp.academics.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByClassIdOrderByNameAsc(Long classId);
    List<Section> findByAcademicYearIdOrderByClassIdAscNameAsc(Long academicYearId);
}
