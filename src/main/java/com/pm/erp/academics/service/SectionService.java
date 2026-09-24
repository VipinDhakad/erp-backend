package com.pm.erp.academics.service;

import com.pm.erp.academics.api.AcademicsDto;
import com.pm.erp.academics.domain.Section;
import com.pm.erp.academics.domain.SectionRepository;
import com.pm.erp.common.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectionService {

    private final SectionRepository repo;

    @Transactional(readOnly = true)
    public List<AcademicsDto.SectionResponse> listByClass(Long classId) {
        return repo.findByClassIdOrderByNameAsc(classId).stream().map(this::toDto).toList();
    }

    @Transactional
    public AcademicsDto.SectionResponse create(AcademicsDto.SectionRequest req) {
        Section s = Section.builder()
                .classId(req.classId())
                .academicYearId(req.academicYearId())
                .name(req.name())
                .build();
        Section saved = repo.save(s);
        log.info("Created section id={} classId={} name={}", saved.getId(), saved.getClassId(), saved.getName());
        return toDto(saved);
    }

    public Section get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Section not found: " + id));
    }

    private AcademicsDto.SectionResponse toDto(Section s) {
        return new AcademicsDto.SectionResponse(s.getId(), s.getClassId(), s.getAcademicYearId(), s.getName());
    }
}
