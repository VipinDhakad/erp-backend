package com.pm.erp.academics.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "class_subject")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ClassSubject.PK.class)
public class ClassSubject {

    @Id
    @Column(name = "class_id")
    private Long classId;

    @Id
    @Column(name = "subject_id")
    private Long subjectId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PK implements Serializable {
        private Long classId;
        private Long subjectId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(classId, pk.classId) && Objects.equals(subjectId, pk.subjectId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(classId, subjectId);
        }
    }
}
