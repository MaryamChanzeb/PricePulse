package com.pricepulse.security;

import com.pricepulse.model.Role;

/**
 * Who is calling, read from a valid token. Controllers receive it with @AuthenticationPrincipal
 * and pass the id to the services; they never trust a user id sent in the request body.
 */
public record AuthenticatedUser(Long id, String email, Role role) {
}
