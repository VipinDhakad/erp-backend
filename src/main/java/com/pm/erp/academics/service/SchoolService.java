package com.pm.erp.academics.service;

import com.pm.erp.academics.api.AcademicsDto;
import com.pm.erp.academics.domain.School;
import com.pm.erp.academics.domain.SchoolRepository;
import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.common.tenant.SchoolContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository repo;
    private final SchoolContext schoolContext;

    @Transactional(readOnly = true)
    public AcademicsDto.SchoolResponse get() {
        return toDto(currentSchool());
    }

    @Transactional
    public AcademicsDto.SchoolResponse update(AcademicsDto.SchoolRequest req) {
        School school = currentSchool();
        school.setName(req.name());
        school.setAddress(req.address());
        school.setPhone(req.phone());
        school.setLogoUrl(req.logoUrl());
        school.setPrincipalName(req.principalName());
        School saved = repo.save(school);
        log.info("Updated school id={} name={}", saved.getId(), saved.getName());
        return toDto(saved);
    }

    private School currentSchool() {
        Long schoolId = schoolContext.currentSchoolId();
        return repo.findById(schoolId).orElseThrow(() -> new NotFoundException("School not found: " + schoolId));
    }

    private AcademicsDto.SchoolResponse toDto(School s) {
        return new AcademicsDto.SchoolResponse(
                s.getId(), s.getName(), s.getCode(), s.getAddress(), s.getPhone(), s.getLogoUrl(), s.getPrincipalName()
        );
    }
}
