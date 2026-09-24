package com.pm.erp.academics.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findBySchoolIdOrderByNameAsc(Long schoolId);
    boolean existsBySchoolIdAndCodeIgnoreCase(Long schoolId, String code);
}
