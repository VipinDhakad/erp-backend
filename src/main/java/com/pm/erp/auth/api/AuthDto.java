package com.pm.erp.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDto {

    private AuthDto() {}

    @Schema(name = "LoginRequest")
    public record LoginRequest(
            @Schema(example = "local-admin") @NotBlank @Size(max = 64) String username,
            @Schema(example = "password") @NotBlank @Size(max = 128) String password
    ) {}

    @Schema(name = "LoginResponse")
    public record LoginResponse(String accessToken, String role, long expiresIn) {}

    @Schema(name = "MeResponse")
    public record MeResponse(Long userId, String username, String role, Long schoolId) {}
}
