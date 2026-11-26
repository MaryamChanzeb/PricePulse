package com.pricepulse.dto;

import com.pricepulse.model.Notification;

import java.time.Instant;

/** A notification as JSON, for the list and for the live stream (FR-30, FR-32). */
public record NotificationResponse(Long id, Long productId, String productTitle, String message, boolean read, Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getProduct().getId(),
                notification.getProduct().getTitle(), notification.getMessage(), notification.isRead(),
                notification.getCreatedAt());
    }
}
