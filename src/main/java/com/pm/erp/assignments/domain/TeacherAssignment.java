package com.pm.erp.assignments.domain;

import com.pm.erp.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teacher_assignment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "is_class_teacher", nullable = false)
    private boolean classTeacher;
}
