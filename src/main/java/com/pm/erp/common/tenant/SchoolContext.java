package com.pm.erp.common.tenant;

import com.pm.erp.auth.security.AuthenticatedUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Convenience accessor for the current authenticated user's school id.
 * All feature services should scope reads/writes through this so adding
 * real multi-tenancy later is a single-spot change.
 */
@Component
public class SchoolContext {

    public Long currentSchoolId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof AuthenticatedUser u) {
            return u.schoolId();
        }
        throw new IllegalStateException("No authenticated user in security context");
    }

    public AuthenticatedUser currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof AuthenticatedUser u) {
            return u;
        }
        throw new IllegalStateException("No authenticated user in security context");
    }
}
