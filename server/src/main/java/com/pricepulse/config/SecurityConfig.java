package com.pricepulse.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricepulse.exception.ErrorResponse;
import com.pricepulse.exception.RoleAccessDeniedException;
import com.pricepulse.security.JwtAuthenticationFilter;
import com.pricepulse.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Who may call what (FR-06, NFR-02, SDS 6 and 10). Role checks happen HERE on the server;
 * hiding a button in the page is never relied on.
 *
 *   /api/auth/signup, /api/auth/login   anyone
 *   /api/admin/**                       ADMIN only   (a shopper gets 403 "Access denied for your role")
 *   /api/**                             SHOPPER only (the "Role" column of SDS 6)
 *   everything else                     public static files: dashboard pages and the demo store.
 *                                       They contain no data; all data comes from /api, which is protected.
 *
 * CORS: no CORS rules are added, so browsers only allow pages from this same server (the dashboard).
 * The extension's background worker is allowed by its host_permissions, not by CORS (SDS 10).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
                .csrf(csrf -> csrf.disable())            // no cookies are used, the token is sent by hand
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").hasRole("SHOPPER")
                        .anyRequest().permitAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Please log in first"))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, RoleAccessDeniedException.MESSAGE)))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** BCrypt: slow on purpose and salted, so stolen hashes are hard to reverse (FR-03). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Security errors happen before any controller runs, so they are written here in the same { status, message } shape. */
    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(status, message));
    }
}
