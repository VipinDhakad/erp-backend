package com.pm.erp.auth.api;

import com.pm.erp.auth.security.AuthenticatedUser;
import com.pm.erp.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/login")
    public AuthDto.LoginResponse login(@RequestBody @Valid AuthDto.LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/auth/me")
    public AuthDto.MeResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return new AuthDto.MeResponse(user.userId(), user.username(), user.role(), user.schoolId());
    }
}
