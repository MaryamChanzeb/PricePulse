package com.pricepulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Entry point: starts Spring, which scans com.pricepulse.* for controllers, services, repositories etc.
// UserDetailsServiceAutoConfiguration is switched off because Price Pulse has its own login (AuthService + JWT);
// otherwise Spring would create an unused default user and print a generated password at startup.
// @ConfigurationPropertiesScan finds RuleConfig and fills it from the pricepulse.rules.* properties.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class PricePulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(PricePulseApplication.class, args);
    }
}
