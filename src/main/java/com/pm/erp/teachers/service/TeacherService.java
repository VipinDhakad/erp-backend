package com.pm.erp.teachers.service;

import com.pm.erp.auth.domain.AppUser;
import com.pm.erp.auth.domain.AppUserRepository;
import com.pm.erp.auth.domain.Role;
import com.pm.erp.auth.domain.RoleRepository;
import com.pm.erp.common.error.ConflictException;
import com.pm.erp.common.error.NotFoundException;
import com.pm.erp.common.tenant.SchoolContext;
import com.pm.erp.teachers.api.TeacherDto;
import com.pm.erp.teachers.domain.Teacher;
import com.pm.erp.teachers.domain.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepo;
    private final AppUserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final SchoolContext schoolContext;

    @Transactional(readOnly = true)
    public List<TeacherDto.TeacherResponse> list() {
        Long schoolId = schoolContext.currentSchoolId();
        List<Teacher> teachers = teacherRepo.findBySchoolIdOrderByLastNameAscFirstNameAsc(schoolId);
        Map<Long, String> usernameByUserId = new HashMap<>();
        teachers.forEach(t -> {
            if (t.getUserId() != null) {
                userRepo.findById(t.getUserId()).ifPresent(u -> usernameByUserId.put(u.getId(), u.getUsername()));
            }
        });
        return teachers.stream().map(t -> new TeacherDto.TeacherResponse(
                t.getId(), t.getFirstName(), t.getLastName(), t.getEmployeeNo(),
                t.getUserId() == null ? null : usernameByUserId.get(t.getUserId())
        )).toList();
    }

    @Transactional
    public TeacherDto.TeacherResponse create(TeacherDto.TeacherRequest req) {
        Long schoolId = schoolContext.currentSchoolId();
        if (userRepo.existsByUsernameIgnoreCase(req.username())) {
            throw new ConflictException("Username already exists: " + req.username());
        }
        Role teacherRole = roleRepo.findByName(Role.TEACHER)
                .orElseThrow(() -> new NotFoundException("Role TEACHER missing — check seed data"));

        AppUser user = AppUser.builder()
                .schoolId(schoolId)
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .enabled(true)
                .role(teacherRole)
                .build();
        AppUser savedUser = userRepo.save(user);

        Teacher teacher = Teacher.builder()
                .schoolId(schoolId)
                .userId(savedUser.getId())
                .firstName(req.firstName())
                .lastName(req.lastName())
                .employeeNo(req.employeeNo())
                .build();
        Teacher saved = teacherRepo.save(teacher);
        log.info("Created teacher id={} userId={}", saved.getId(), savedUser.getId());
        return new TeacherDto.TeacherResponse(saved.getId(), saved.getFirstName(), saved.getLastName(),
                saved.getEmployeeNo(), savedUser.getUsername());
    }
}
