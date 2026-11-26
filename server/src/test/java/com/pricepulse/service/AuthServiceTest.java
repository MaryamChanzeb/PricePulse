package com.pricepulse.service;

import com.pricepulse.TestUsers;
import com.pricepulse.dto.AuthResponse;
import com.pricepulse.exception.AuthenticationFailedException;
import com.pricepulse.exception.DuplicateEmailException;
import com.pricepulse.exception.ValidationException;
import com.pricepulse.model.Admin;
import com.pricepulse.model.Role;
import com.pricepulse.model.Shopper;
import com.pricepulse.model.User;
import com.pricepulse.repository.UserRepository;
import com.pricepulse.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** AuthService with a mocked repository: no database needed (NFR-08). */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4); // low cost: fast tests
    private final JwtService jwtService = new JwtService(TestUsers.SECRET, 24);
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, encoder, jwtService);
    }

    @Test
    void signupCreatesShopperWithBcryptHashAndReturnsToken() {
        when(userRepository.existsByEmail("sara@mail.pk")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(call -> TestUsers.withId(call.getArgument(0), 5L));

        AuthResponse response = authService.signup("Sara", "  Sara@Mail.PK ", "secret123");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue()).isInstanceOf(Shopper.class);
        assertThat(saved.getValue().getEmail()).isEqualTo("sara@mail.pk");
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("secret123").startsWith("$2");
        assertThat(encoder.matches("secret123", saved.getValue().getPasswordHash())).isTrue();

        assertThat(response.role()).isEqualTo(Role.SHOPPER);
        assertThat(response.dashboardPath()).isEqualTo("/dashboard.html");
        assertThat(jwtService.readToken(response.token())).isPresent();
    }

    @Test
    void signupWithExistingEmailIsRefused() {
        when(userRepository.existsByEmail("sara@mail.pk")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup("Sara", "sara@mail.pk", "secret123"))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("This email is already registered");
        verify(userRepository, never()).save(any());
    }

    @Test
    void weakPasswordsAreRefused() {
        assertThatThrownBy(() -> authService.signup("Sara", "sara@mail.pk", "short1"))
                .isInstanceOf(ValidationException.class).hasMessage(AuthService.PASSWORD_RULE_MESSAGE);
        assertThatThrownBy(() -> authService.signup("Sara", "sara@mail.pk", "longbutnodigit"))
                .isInstanceOf(ValidationException.class).hasMessage(AuthService.PASSWORD_RULE_MESSAGE);
        verify(userRepository, never()).save(any());
    }

    @Test
    void passwordOfExactlyEightCharactersWithDigitIsAccepted() {
        when(userRepository.existsByEmail("sara@mail.pk")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(call -> TestUsers.withId(call.getArgument(0), 5L));

        assertThat(authService.signup("Sara", "sara@mail.pk", "abcdefg1").role()).isEqualTo(Role.SHOPPER);
    }

    @Test
    void invalidEmailOrBlankNameIsRefused() {
        assertThatThrownBy(() -> authService.signup("Sara", "not-an-email", "secret123"))
                .isInstanceOf(ValidationException.class).hasMessage("Enter a valid email address");
        assertThatThrownBy(() -> authService.signup("  ", "sara@mail.pk", "secret123"))
                .isInstanceOf(ValidationException.class).hasMessage("Name is required");
    }

    @Test
    void loginWithCorrectPasswordReturnsTokenAndDashboardPathOfTheSubclass() {
        Admin admin = TestUsers.withId(new Admin("Ali", "ali@mail.pk", encoder.encode("admin1234")), 9L);
        when(userRepository.findByEmail("ali@mail.pk")).thenReturn(Optional.of(admin));

        AuthResponse response = authService.login("Ali@mail.pk", "admin1234");

        assertThat(response.role()).isEqualTo(Role.ADMIN);
        assertThat(response.dashboardPath()).isEqualTo("/admin.html");
        assertThat(jwtService.readToken(response.token()).orElseThrow().id()).isEqualTo(9L);
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameMessage() {
        Shopper sara = TestUsers.withId(new Shopper("Sara", "sara@mail.pk", encoder.encode("secret123")), 5L);
        when(userRepository.findByEmail("sara@mail.pk")).thenReturn(Optional.of(sara));
        when(userRepository.findByEmail("nobody@mail.pk")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("sara@mail.pk", "wrong-pass1"))
                .isInstanceOf(AuthenticationFailedException.class).hasMessage("Invalid email or password");
        assertThatThrownBy(() -> authService.login("nobody@mail.pk", "secret123"))
                .isInstanceOf(AuthenticationFailedException.class).hasMessage("Invalid email or password");
    }

    @Test
    void adminAccountIsCreatedOnlyOnce() {
        when(userRepository.existsByEmail("admin@pp.pk")).thenReturn(false, true);
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(authService.ensureAdminAccount("Admin", "admin@pp.pk", "admin1234")).isTrue();
        assertThat(authService.ensureAdminAccount("Admin", "admin@pp.pk", "admin1234")).isFalse();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue()).isInstanceOf(Admin.class);
    }
}
