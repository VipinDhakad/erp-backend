package com.pm.erp.teachers.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    List<Teacher> findBySchoolIdOrderByLastNameAscFirstNameAsc(Long schoolId);
    Optional<Teacher> findByUserId(Long userId);
}
