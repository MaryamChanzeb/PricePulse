package com.pricepulse.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void callsTheOverriddenMethodsThroughTheUserType() {
        // Both objects are held as the parent type User; Java runs the subclass method (polymorphism).
        List<User> users = List.of(
                new Shopper("Sara", "sara@mail.pk", "hash"),
                new Admin("Ali", "ali@mail.pk", "hash"));

        assertThat(users.get(0).getDashboardPath()).isEqualTo("/dashboard.html");
        assertThat(users.get(0).canReviewObservations()).isFalse();
        assertThat(users.get(0).getRole()).isEqualTo(Role.SHOPPER);

        assertThat(users.get(1).getDashboardPath()).isEqualTo("/admin.html");
        assertThat(users.get(1).canReviewObservations()).isTrue();
        assertThat(users.get(1).getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void rejectsBlankFields() {
        assertThatThrownBy(() -> new Shopper(" ", "a@b.pk", "hash")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Shopper("Sara", null, "hash")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Shopper("Sara", "a@b.pk", "")).isInstanceOf(IllegalArgumentException.class);
    }
}
