package com.pricepulse.controller;

import com.pricepulse.TestUsers;
import com.pricepulse.config.SecurityConfig;
import com.pricepulse.dto.AuthResponse;
import com.pricepulse.exception.AuthenticationFailedException;
import com.pricepulse.model.Role;
import com.pricepulse.security.JwtService;
import com.pricepulse.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The real security chain and exception handler with a mocked AuthService (FR-05, FR-06, FR-40).
 * No database is needed.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtService.class})
@TestPropertySource(properties = "pricepulse.jwt.secret=" + TestUsers.SECRET)
class SecurityRulesTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void adminEndpointWithoutTokenGives401Json() throws Exception {
        mvc.perform(get("/api/admin/flagged"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Please log in first"));
    }

    @Test
    void shopperCallingAdminEndpointGets403() throws Exception {
        String token = jwtService.createToken(TestUsers.shopper(1L, "s@pp.pk"));
        mvc.perform(get("/api/admin/flagged").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied for your role"));
    }

    @Test
    void adminPassesTheRoleCheck() throws Exception {
        // No admin controller exists yet, so passing security ends in our JSON 404 instead of 401/403.
        String token = jwtService.createToken(TestUsers.admin(2L, "a@pp.pk"));
        mvc.perform(get("/api/admin/flagged").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Not found"));
    }

    @Test
    void adminCannotUseShopperEndpoints() throws Exception {
        String token = jwtService.createToken(TestUsers.admin(2L, "a@pp.pk"));
        mvc.perform(get("/api/watchlist").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidTokenIsTreatedAsNotLoggedIn() throws Exception {
        mvc.perform(get("/api/watchlist").header("Authorization", bearer("forged.token.value")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginIsPublicAndReturnsToken() throws Exception {
        when(authService.login("sara@mail.pk", "secret123"))
                .thenReturn(new AuthResponse("tok", "Sara", "sara@mail.pk", Role.SHOPPER, "/dashboard.html"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"sara@mail.pk\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("tok"))
                .andExpect(jsonPath("$.dashboardPath").value("/dashboard.html"));
    }

    @Test
    void wrongLoginGives401WithTheSrsMessage() throws Exception {
        when(authService.login(any(), any())).thenThrow(new AuthenticationFailedException());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"sara@mail.pk\",\"password\":\"wrong1234\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void signupWithMissingEmailGivesFieldMessage() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sara\",\"email\":\"\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is required"));
    }

    @Test
    void brokenJsonGives400NotAStackTrace() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The request is not valid JSON"));
    }

    @Test
    void unexpectedServerErrorHidesDetails() throws Exception {
        when(authService.login(any(), any())).thenThrow(new IllegalStateException("secret internal detail"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"sara@mail.pk\",\"password\":\"secret123\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong, please try again"));
    }
}
