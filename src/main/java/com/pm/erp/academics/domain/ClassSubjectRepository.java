package com.pm.erp.academics.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassSubjectRepository extends JpaRepository<ClassSubject, ClassSubject.PK> {
    List<ClassSubject> findByClassId(Long classId);
    void deleteByClassId(Long classId);
}
