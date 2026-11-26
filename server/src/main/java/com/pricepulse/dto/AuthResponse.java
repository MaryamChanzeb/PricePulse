package com.pricepulse.dto;

import com.pricepulse.model.Role;

/**
 * Answer to sign-up and login: the token for the Authorization header, who the user is,
 * and where the dashboard should send them (User.getDashboardPath(), SDS 3.1).
 */
public record AuthResponse(String token, String name, String email, Role role, String dashboardPath) {
}
