package com.pricepulse.repository;

import com.pricepulse.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Database access for users. Spring Data writes the SQL from the method names, always as
 * parameterised queries (NFR-02), so user input can never change the SQL itself.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Login (FR-04). Returns a Shopper or an Admin object depending on the role column. */
    Optional<User> findByEmail(String email);

    /** Sign-up check for FR-02. */
    boolean existsByEmail(String email);
}
