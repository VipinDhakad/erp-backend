package com.pm.erp.academics.service;

import com.pm.erp.academics.api.AcademicsDto;
import com.pm.erp.academics.domain.ClassSubjectRepository;
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
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository repo;
    private final ClassSubjectRepository classSubjectRepo;
    private final SchoolContext schoolContext;

    @Transactional(readOnly = true)
    public List<AcademicsDto.SubjectResponse> list(Long classId) {
        Long schoolId = schoolContext.currentSchoolId();
        List<Subject> subjects = repo.findBySchoolIdOrderByNameAsc(schoolId);
        if (classId == null) {
            return subjects.stream().map(this::toDto).toList();
        }
        Set<Long> subjectIdsForClass = classSubjectRepo.findByClassId(classId).stream()
                .map(com.pm.erp.academics.domain.ClassSubject::getSubjectId)
                .collect(java.util.stream.Collectors.toSet());
        return subjects.stream()
                .filter(s -> subjectIdsForClass.contains(s.getId()))
                .map(this::toDto)
                .toList();
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

    @Transactional
    public AcademicsDto.SubjectResponse update(Long id, AcademicsDto.SubjectUpdateRequest req) {
        Long schoolId = schoolContext.currentSchoolId();
        Subject s = repo.findById(id)
                .filter(x -> x.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new NotFoundException("Subject not found: " + id));
        if (!s.getCode().equalsIgnoreCase(req.code()) && repo.existsBySchoolIdAndCodeIgnoreCase(schoolId, req.code())) {
            throw new ConflictException("Subject already exists with code: " + req.code());
        }
        s.setName(req.name());
        s.setCode(req.code());
        s.setMaxMarks(req.maxMarks());
        Subject saved = repo.save(s);
        log.info("Updated subject id={} code={}", saved.getId(), saved.getCode());
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long schoolId = schoolContext.currentSchoolId();
        Subject s = repo.findById(id)
                .filter(x -> x.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new NotFoundException("Subject not found: " + id));
        if (classSubjectRepo.existsBySubjectId(id)) {
            throw new ConflictException("Cannot delete a subject still assigned to a class — unassign it first");
        }
        repo.delete(s);
        log.info("Deleted subject id={}", id);
    }

    private AcademicsDto.SubjectResponse toDto(Subject s) {
        return new AcademicsDto.SubjectResponse(s.getId(), s.getName(), s.getCode(), s.getMaxMarks());
    }
}
