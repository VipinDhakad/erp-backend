package com.pm.erp.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Local-profile auth shim. Every request is treated as if it came from a synthetic admin user
 * so developers can call protected endpoints without going through /auth/login.
 * Never registered when SPRING_PROFILES_ACTIVE is e2e or prod.
 */
@Slf4j
@RequiredArgsConstructor
public class LocalAuthFilter extends OncePerRequestFilter {

    private final LocalAuthProperties props;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        AuthenticatedUser principal = new AuthenticatedUser(
                props.userId(), props.username(), props.role(), props.schoolId());
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + props.role()))
        );
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
        log.debug("[local-auth] {} {} -> stamped principal user={} role={} schoolId={}",
                request.getMethod(), request.getRequestURI(),
                props.username(), props.role(), props.schoolId());
        chain.doFilter(request, response);
    }
}
