package com.pm.erp.auth.security;

import com.pm.erp.auth.jwt.InvalidJwtException;
import com.pm.erp.auth.jwt.JwtService;
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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads "Authorization: Bearer ..." and populates the SecurityContext with the parsed user.
 * Stateless — does not touch the database. Active only on e2e/prod profiles.
 */
@Slf4j
@Component
@Profile({"e2e", "prod"})
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "Authorization";
    public static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String header = request.getHeader(HEADER);

        if (!StringUtils.hasText(header)) {
            log.info("{} {} -> no Authorization header (anonymous)", method, uri);
        } else if (!header.startsWith(PREFIX)) {
            log.info("{} {} -> Authorization header present but not Bearer (anonymous)", method, uri);
        } else {
            String token = header.substring(PREFIX.length()).trim();
            try {
                JwtService.ParsedToken parsed = jwtService.parse(token);
                AuthenticatedUser principal = new AuthenticatedUser(
                        parsed.userId(), parsed.username(), parsed.role(), parsed.schoolId()
                );
                var auth = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + parsed.role()))
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.info("{} {} -> authenticated as user={} role={} schoolId={}",
                        method, uri, parsed.username(), parsed.role(), parsed.schoolId());
            } catch (InvalidJwtException ex) {
                SecurityContextHolder.clearContext();
                log.warn("{} {} -> rejected: invalid JWT ({})", method, uri, ex.getMessage());
            }
        }
        chain.doFilter(request, response);
        log.info("{} {} -> status={}", method, uri, response.getStatus());
    }
}
