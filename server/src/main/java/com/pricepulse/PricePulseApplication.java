package com.pricepulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

// Entry point: starts Spring, which scans com.pricepulse.* for controllers, services, repositories etc.
// UserDetailsServiceAutoConfiguration is switched off because Price Pulse has its own login (AuthService + JWT);
// otherwise Spring would create an unused default user and print a generated password at startup.
// @ConfigurationPropertiesScan finds RuleConfig and fills it from the pricepulse.rules.* properties.
// @EnableScheduling runs NotificationService.keepAlive() every 25 seconds for the live streams.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@EnableScheduling
public class PricePulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(PricePulseApplication.class, args);
    }
}
