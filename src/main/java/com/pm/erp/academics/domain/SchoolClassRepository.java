package com.pm.erp.academics.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {
    List<SchoolClass> findBySchoolIdOrderByDisplayOrderAsc(Long schoolId);
    boolean existsBySchoolIdAndNameIgnoreCase(Long schoolId, String name);
}
