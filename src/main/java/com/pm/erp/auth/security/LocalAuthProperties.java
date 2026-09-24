package com.pm.erp.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.local-user")
public record LocalAuthProperties(
        Long userId,
        String username,
        String role,
        Long schoolId
) {}
