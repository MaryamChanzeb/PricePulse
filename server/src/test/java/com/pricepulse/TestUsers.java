package com.pricepulse;

import com.pricepulse.model.Admin;
import com.pricepulse.model.Shopper;
import com.pricepulse.model.User;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test helpers. Entities get their id from the database; in unit tests there is no database,
 * so the id is set with reflection (test code only - production code has no setId()).
 */
public final class TestUsers {

    public static final String SECRET = "test-secret-that-is-at-least-32-characters-long";

    private TestUsers() {
    }

    public static Shopper shopper(long id, String email) {
        return withId(new Shopper("Test Shopper", email, "hash"), id);
    }

    public static Admin admin(long id, String email) {
        return withId(new Admin("Test Admin", email, "hash"), id);
    }

    public static <T extends User> T withId(T user, long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
