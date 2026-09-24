package com.pm.erp.academics.service;

import com.pm.erp.academics.api.AcademicsDto;
import com.pm.erp.academics.domain.AcademicYear;
import com.pm.erp.academics.domain.AcademicYearRepository;
import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.common.tenant.SchoolContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicYearService {

    private final AcademicYearRepository repo;
    private final SchoolContext schoolContext;

    @Transactional(readOnly = true)
    public List<AcademicsDto.AcademicYearResponse> list() {
        Long schoolId = schoolContext.currentSchoolId();
        return repo.findBySchoolIdOrderByStartDateDesc(schoolId).stream().map(this::toDto).toList();
    }

    @Transactional
    public AcademicsDto.AcademicYearResponse create(AcademicsDto.AcademicYearRequest req) {
        Long schoolId = schoolContext.currentSchoolId();
        AcademicYear y = AcademicYear.builder()
                .schoolId(schoolId)
                .name(req.name())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .current(req.current())
                .build();
        AcademicYear saved = repo.save(y);
        log.info("Created academic year id={} name={} schoolId={}", saved.getId(), saved.getName(), schoolId);
        return toDto(saved);
    }

    public AcademicYear get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Academic year not found: " + id));
    }

    private AcademicsDto.AcademicYearResponse toDto(AcademicYear y) {
        return new AcademicsDto.AcademicYearResponse(y.getId(), y.getName(), y.getStartDate(), y.getEndDate(), y.isCurrent());
    }
}
