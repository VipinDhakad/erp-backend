package com.pm.erp.auth.security;

import java.io.Serializable;

public record AuthenticatedUser(Long userId, String username, String role, Long schoolId) implements Serializable {}
