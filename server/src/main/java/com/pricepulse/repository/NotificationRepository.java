package com.pricepulse.repository;

import com.pricepulse.model.Notification;
import com.pricepulse.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Number on the extension icon (FR-31). Uses the (user_id, is_read) index. */
    long countByUserAndReadFalse(User user);

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    /** A notification only its owner may mark as read (FR-32). */
    Optional<Notification> findByIdAndUser(Long id, User user);
}
