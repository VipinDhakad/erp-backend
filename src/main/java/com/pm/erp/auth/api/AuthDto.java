package com.pm.erp.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDto {

    private AuthDto() {}

    public record LoginRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 128) String password
    ) {}

    public record LoginResponse(String accessToken, String role, long expiresIn) {}

    public record MeResponse(Long userId, String username, String role, Long schoolId) {}
}
