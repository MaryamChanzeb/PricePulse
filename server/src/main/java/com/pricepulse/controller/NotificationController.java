package com.pricepulse.controller;

import com.pricepulse.dto.NotificationResponse;
import com.pricepulse.security.AuthenticatedUser;
import com.pricepulse.service.NotificationService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/** Notifications (SDS 6): list, live stream (FR-30), unread count (FR-31), mark as read (FR-32). Shopper role. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal AuthenticatedUser caller) {
        return notificationService.list(caller.id());
    }

    /** Server-Sent Events. The dashboard reads it with fetch() so it can send the Authorization header. */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal AuthenticatedUser caller) {
        return notificationService.subscribe(caller.id());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AuthenticatedUser caller) {
        return Map.of("count", notificationService.unreadCount(caller.id()));
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markRead(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable("id") Long notificationId) {
        return notificationService.markRead(caller.id(), notificationId);
    }
}
