package com.pricepulse.service;

import com.pricepulse.dto.AuthResponse;
import com.pricepulse.exception.AuthenticationFailedException;
import com.pricepulse.exception.DuplicateEmailException;
import com.pricepulse.exception.ValidationException;
import com.pricepulse.model.Admin;
import com.pricepulse.model.Shopper;
import com.pricepulse.model.User;
import com.pricepulse.repository.UserRepository;
import com.pricepulse.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Sign-up, login and the startup Admin account (FR-01 to FR-05, SRS 2.2). */
@Service
public class AuthService {

    static final int MIN_PASSWORD_LENGTH = 8;
    static final String PASSWORD_RULE_MESSAGE = "Password must have at least 8 characters and at least one digit";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Compared against when the email is unknown, so a wrong email takes as long as a wrong password. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("not-a-real-password-1");
    }

    /** FR-01 to FR-03: public sign-up always creates a SHOPPER. */
    @Transactional
    public AuthResponse signup(String name, String email, String password) {
        String cleanName = requireName(name);
        String cleanEmail = normaliseEmail(email);
        requireStrongPassword(password);
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new DuplicateEmailException();
        }
        User saved = userRepository.save(new Shopper(cleanName, cleanEmail, passwordEncoder.encode(password)));
        return toResponse(saved);
    }

    /** FR-04, FR-05. Same message for an unknown email and a wrong password. */
    @Transactional(readOnly = true)
    public AuthResponse login(String email, String password) {
        if (email == null || password == null) {
            throw new AuthenticationFailedException();
        }
        Optional<User> found = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (found.isEmpty()) {
            passwordEncoder.matches(password, dummyHash); // spend the same time as a real check
            throw new AuthenticationFailedException();
        }
        User user = found.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthenticationFailedException();
        }
        return toResponse(user);
    }

    /**
     * Creates the Admin account if no user with this email exists (called once at startup).
     * @return true if a new Admin was created, false if the email was already registered
     */
    @Transactional
    public boolean ensureAdminAccount(String name, String email, String password) {
        String cleanEmail = normaliseEmail(email);
        requireStrongPassword(password);
        if (userRepository.existsByEmail(cleanEmail)) {
            return false;
        }
        userRepository.save(new Admin(requireName(name), cleanEmail, passwordEncoder.encode(password)));
        return true;
    }

    private AuthResponse toResponse(User user) {
        // user.getDashboardPath() is called on the User type: Shopper and Admin each answer differently.
        return new AuthResponse(jwtService.createToken(user), user.getName(), user.getEmail(),
                user.getRole(), user.getDashboardPath());
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Name is required");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 100) {
            throw new ValidationException("Name must be at most 100 characters");
        }
        return trimmed;
    }

    private String normaliseEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required");
        }
        String cleaned = email.trim().toLowerCase(Locale.ROOT);
        if (cleaned.length() > 255 || !EMAIL_PATTERN.matcher(cleaned).matches()) {
            throw new ValidationException("Enter a valid email address");
        }
        return cleaned;
    }

    private void requireStrongPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || !HAS_DIGIT.matcher(password).matches()) {
            throw new ValidationException(PASSWORD_RULE_MESSAGE);
        }
    }
}
