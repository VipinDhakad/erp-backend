package com.pm.erp.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Issues and parses HS256 JWTs.
 * <p>
 * Single responsibility: token lifecycle. Does not know about HTTP, Spring Security, or users —
 * callers pass primitive claim values in and out.
 */
@Slf4j
@Service
public class JwtService {

    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_SCHOOL_ID = "schoolId";
    public static final String CLAIM_USERNAME = "username";

    private final JwtProperties props;
    private final SecretKey signingKey;

    public JwtService(JwtProperties props) {
        this.props = props;
        byte[] keyBytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public IssuedToken issueAccessToken(Long userId, String username, String role, Long schoolId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.accessTokenTtlSeconds());
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(props.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claims(Map.of(
                        CLAIM_USERNAME, username,
                        CLAIM_ROLE, role,
                        CLAIM_SCHOOL_ID, schoolId
                ))
                .signWith(signingKey)
                .compact();
        // Do not log the token itself; only its lifetime.
        log.debug("Issued access token for userId={} ttlSeconds={}", userId, props.accessTokenTtlSeconds());
        return new IssuedToken(token, props.accessTokenTtlSeconds());
    }

    public ParsedToken parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(props.issuer())
                    .build()
                    .parseSignedClaims(token);
            Claims c = jws.getPayload();
            return new ParsedToken(
                    Long.parseLong(c.getSubject()),
                    c.get(CLAIM_USERNAME, String.class),
                    c.get(CLAIM_ROLE, String.class),
                    c.get(CLAIM_SCHOOL_ID, Long.class)
            );
        } catch (JwtException | IllegalArgumentException ex) {
            // Generic log line; never include token in logs.
            log.debug("JWT parse failed: {}", ex.getClass().getSimpleName());
            throw new InvalidJwtException("Invalid or expired token", ex);
        }
    }

    public record IssuedToken(String token, long expiresInSeconds) {}

    public record ParsedToken(Long userId, String username, String role, Long schoolId) {}
}
