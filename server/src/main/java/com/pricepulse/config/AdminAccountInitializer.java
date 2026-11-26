package com.pricepulse.config;

import com.pricepulse.exception.ValidationException;
import com.pricepulse.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the Admin account when the server starts (SRS 2.2: no public admin sign-up).
 * The email and password come from ADMIN_EMAIL and ADMIN_PASSWORD, so no admin password
 * or hash is ever stored in a committed file. The password itself is never logged.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final AuthService authService;
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;

    public AdminAccountInitializer(AuthService authService,
                                   @Value("${pricepulse.admin.name:Price Pulse Admin}") String adminName,
                                   @Value("${pricepulse.admin.email:}") String adminEmail,
                                   @Value("${pricepulse.admin.password:}") String adminPassword) {
        this.authService = authService;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("ADMIN_EMAIL or ADMIN_PASSWORD is not set: no admin account was created.");
            return;
        }
        try {
            boolean created = authService.ensureAdminAccount(adminName, adminEmail, adminPassword);
            log.info(created ? "Admin account created for {}" : "Admin account {} already exists", adminEmail);
        } catch (ValidationException invalid) {
            log.error("Admin account NOT created: {}", invalid.getMessage());
        }
    }
}
