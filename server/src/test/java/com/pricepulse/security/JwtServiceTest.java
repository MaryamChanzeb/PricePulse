package com.pricepulse.security;

import com.pricepulse.TestUsers;
import com.pricepulse.model.Role;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");

    private JwtService serviceAt(Instant time) {
        return new JwtService(TestUsers.SECRET, Duration.ofHours(24), Clock.fixed(time, ZoneOffset.UTC));
    }

    @Test
    void tokenRoundTripKeepsIdEmailAndRole() {
        JwtService jwt = serviceAt(NOW);
        String token = jwt.createToken(TestUsers.admin(7L, "admin@pp.pk"));

        Optional<AuthenticatedUser> user = jwt.readToken(token);

        assertThat(user).isPresent();
        assertThat(user.get().id()).isEqualTo(7L);
        assertThat(user.get().email()).isEqualTo("admin@pp.pk");
        assertThat(user.get().role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void changedTokenIsRefused() {
        JwtService jwt = serviceAt(NOW);
        String token = jwt.createToken(TestUsers.shopper(1L, "s@pp.pk"));
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThat(jwt.readToken(tampered)).isEmpty();
        assertThat(jwt.readToken("not-a-token")).isEmpty();
        assertThat(jwt.readToken("")).isEmpty();
    }

    @Test
    void tokenIsValidFor24HoursOnly() {
        String token = serviceAt(NOW).createToken(TestUsers.shopper(1L, "s@pp.pk"));

        assertThat(serviceAt(NOW.plus(Duration.ofHours(23))).readToken(token)).isPresent();
        assertThat(serviceAt(NOW.plus(Duration.ofHours(25))).readToken(token)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRefused() {
        String token = serviceAt(NOW).createToken(TestUsers.shopper(1L, "s@pp.pk"));
        JwtService other = new JwtService("another-secret-that-is-at-least-32-chars!", Duration.ofHours(24),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(other.readToken(token)).isEmpty();
    }

    @Test
    void missingOrShortSecretStopsStartup() {
        assertThatThrownBy(() -> new JwtService("", 24)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService("too-short", 24)).isInstanceOf(IllegalStateException.class);
    }
}
