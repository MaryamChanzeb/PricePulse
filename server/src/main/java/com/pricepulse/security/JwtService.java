package com.pricepulse.security;

import com.pricepulse.model.Role;
import com.pricepulse.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Creates and checks login tokens (JWT, FR-04).
 *
 * A token is three parts: header.payload.signature. The payload says who the user is and when the token
 * expires; the signature is made with our secret key (HMAC-SHA256). If anyone changes even one character
 * of the payload, the signature no longer matches and the token is refused. The server keeps no session:
 * the signed token itself is the proof (SDS 7, "Token based login").
 */
@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32; // HS256 needs a key of at least 256 bits
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final Duration validity;
    private final Clock clock;

    @Autowired
    public JwtService(@Value("${pricepulse.jwt.secret:}") String secret,
                      @Value("${pricepulse.jwt.expiration-hours:24}") long expirationHours) {
        this(secret, Duration.ofHours(expirationHours), Clock.systemUTC());
    }

    /** Used by tests to control the clock (for example to test an expired token). */
    JwtService(String secret, Duration validity, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET is missing or shorter than " + MIN_SECRET_BYTES
                    + " characters. Set it as an environment variable or in application-local.properties.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validity = validity;
        this.clock = clock;
    }

    public String createToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(key)
                .compact();
    }

    /** Empty when the token is missing, changed, signed with another key, or expired. */
    public Optional<AuthenticatedUser> readToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            String email = claims.get(CLAIM_EMAIL, String.class);
            Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
            return Optional.of(new AuthenticatedUser(userId, email, role));
        } catch (JwtException | IllegalArgumentException | NullPointerException invalid) {
            return Optional.empty();
        }
    }
}
