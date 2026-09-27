package com.pm.erp.auth.api;

import com.pm.erp.auth.security.AuthenticatedUser;
import com.pm.erp.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Login and current-session identity")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Log in",
            description = "Exchanges a username/password for a JWT access token. Public endpoint — " +
                    "not required on the local profile, where every request is auto-authenticated as an admin.",
            security = {}
    )
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
            value = """
                    {
                      "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiaXNzIjoiZXJwLWJhY2tlbmQiLCJyb2xlIjoiQURNSU4ifQ.signature",
                      "role": "ADMIN",
                      "expiresIn": 28800
                    }
                    """
    )))
    @ApiResponse(responseCode = "401", description = "Invalid username or password", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
            value = """
                    {
                      "timestamp": "2026-09-27T09:30:00Z",
                      "status": 401,
                      "error": "Unauthorized",
                      "message": "Invalid username or password",
                      "path": "/auth/login",
                      "violations": []
                    }
                    """
    )))
    @PostMapping("/auth/login")
    public AuthDto.LoginResponse login(@RequestBody @Valid AuthDto.LoginRequest req) {
        return authService.login(req);
    }

    @Operation(summary = "Get current user", description = "Returns the identity encoded in the caller's bearer token (or the local dev principal).")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
            value = """
                    {
                      "userId": 2,
                      "username": "teacher1",
                      "role": "TEACHER",
                      "schoolId": 1
                    }
                    """
    )))
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/auth/me")
    public AuthDto.MeResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return new AuthDto.MeResponse(user.userId(), user.username(), user.role(), user.schoolId());
    }
}
