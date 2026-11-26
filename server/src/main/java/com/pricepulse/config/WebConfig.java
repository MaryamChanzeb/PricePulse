package com.pricepulse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Boot only serves index.html automatically for the site root "/". This makes the
 * demo store's folder address work too: /demo-store and /demo-store/ show /demo-store/index.html.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/demo-store", "/demo-store/");
        registry.addViewController("/demo-store/").setViewName("forward:/demo-store/index.html");
    }
}
