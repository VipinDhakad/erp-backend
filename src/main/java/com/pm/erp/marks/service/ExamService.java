package com.pm.erp.marks.service;

import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.common.tenant.SchoolContext;
import com.pm.erp.marks.api.MarksDto;
import com.pm.erp.marks.domain.Exam;
import com.pm.erp.marks.domain.ExamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository repo;
    private final SchoolContext schoolContext;

    @Transactional(readOnly = true)
    public List<MarksDto.ExamResponse> list() {
        Long schoolId = schoolContext.currentSchoolId();
        return repo.findBySchoolIdOrderByStartDateDescIdDesc(schoolId).stream().map(this::toDto).toList();
    }

    @Transactional
    public MarksDto.ExamResponse create(MarksDto.ExamRequest req) {
        Long schoolId = schoolContext.currentSchoolId();
        Exam e = Exam.builder()
                .schoolId(schoolId)
                .academicYearId(req.academicYearId())
                .name(req.name())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .build();
        Exam saved = repo.save(e);
        log.info("Created exam id={} name={}", saved.getId(), saved.getName());
        return toDto(saved);
    }

    public Exam get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Exam not found: " + id));
    }

    private MarksDto.ExamResponse toDto(Exam e) {
        return new MarksDto.ExamResponse(e.getId(), e.getName(), e.getStartDate(), e.getEndDate(), e.getAcademicYearId());
    }
}
