package com.pm.erp.academics.service;

import com.pm.erp.academics.api.AcademicsDto;
import com.pm.erp.academics.domain.Subject;
import com.pm.erp.academics.domain.SubjectRepository;
import com.pm.erp.common.error.ConflictException;
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
public class SubjectService {

    private final SubjectRepository repo;
    private final SchoolContext schoolContext;

    @Transactional(readOnly = true)
    public List<AcademicsDto.SubjectResponse> list() {
        Long schoolId = schoolContext.currentSchoolId();
        return repo.findBySchoolIdOrderByNameAsc(schoolId).stream().map(this::toDto).toList();
    }

    @Transactional
    public AcademicsDto.SubjectResponse create(AcademicsDto.SubjectRequest req) {
        Long schoolId = schoolContext.currentSchoolId();
        if (repo.existsBySchoolIdAndCodeIgnoreCase(schoolId, req.code())) {
            throw new ConflictException("Subject already exists with code: " + req.code());
        }
        Subject s = Subject.builder()
                .schoolId(schoolId)
                .name(req.name())
                .code(req.code())
                .maxMarks(req.maxMarks())
                .build();
        Subject saved = repo.save(s);
        log.info("Created subject id={} code={}", saved.getId(), saved.getCode());
        return toDto(saved);
    }

    public Subject get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Subject not found: " + id));
    }

    private AcademicsDto.SubjectResponse toDto(Subject s) {
        return new AcademicsDto.SubjectResponse(s.getId(), s.getName(), s.getCode(), s.getMaxMarks());
    }
}
