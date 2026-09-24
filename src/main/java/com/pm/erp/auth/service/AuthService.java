package com.pm.erp.auth.service;

import com.pm.erp.auth.api.AuthDto;
import com.pm.erp.auth.domain.AppUser;
import com.pm.erp.auth.domain.AppUserRepository;
import com.pm.erp.auth.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authenticates a user against the stored bcrypt hash and issues a JWT on success.
 * SRP: this class does not know about HTTP or the JWT format — it delegates to JwtService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public AuthDto.LoginResponse login(AuthDto.LoginRequest req) {
        AppUser user = userRepo.findByUsernameIgnoreCase(req.username())
                .orElseThrow(() -> {
                    // Generic log — do not include the supplied username (potentially attacker-controlled).
                    log.info("Login failed: unknown user");
                    return new BadCredentialsException("Invalid credentials");
                });
        if (!user.isEnabled()) {
            log.info("Login failed: user disabled (userId={})", user.getId());
            throw new BadCredentialsException("Invalid credentials");
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            log.info("Login failed: bad password (userId={})", user.getId());
            throw new BadCredentialsException("Invalid credentials");
        }
        String roleName = user.getRole().getName();
        JwtService.IssuedToken issued = jwtService.issueAccessToken(
                user.getId(), user.getUsername(), roleName, user.getSchoolId()
        );
        log.info("Login success userId={} role={}", user.getId(), roleName);
        return new AuthDto.LoginResponse(issued.token(), roleName, issued.expiresInSeconds());
    }
}
