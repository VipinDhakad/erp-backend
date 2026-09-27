package com.pm.erp.marks.domain;

import com.pm.erp.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "exam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exam extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "max_marks", nullable = false)
    private int maxMarks;

    @Column(nullable = false)
    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
