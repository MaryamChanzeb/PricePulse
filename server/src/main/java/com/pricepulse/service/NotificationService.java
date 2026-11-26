package com.pricepulse.service;

import com.pricepulse.dto.NotificationResponse;
import com.pricepulse.exception.NotLoggedInException;
import com.pricepulse.exception.ProductNotFoundException;
import com.pricepulse.model.Notification;
import com.pricepulse.model.Product;
import com.pricepulse.model.User;
import com.pricepulse.repository.NotificationRepository;
import com.pricepulse.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Saves notifications and delivers them live (FR-28, FR-30 to FR-32; SDS 3.3 and 7).
 *
 * Server-Sent Events: the dashboard opens ONE long HTTP response (GET /api/notifications/stream) and the
 * server writes an event into it whenever something happens. Alerts only travel server -> browser, so this
 * is simpler than WebSockets. A user can have several tabs open, so the server keeps a
 * Map from user id to the List of that user's open streams.
 */
@Service
public class NotificationService {

    static final long STREAM_TIMEOUT_MS = 30L * 60 * 1000; // the browser reconnects after 30 minutes
    private static final int LIST_LIMIT = 50;
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final Map<Long, List<SseEmitter>> streamsByUser = new ConcurrentHashMap<>();
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // ---------- live stream ----------

    /** Opens a live stream for this user. The first event tells the current unread count. */
    public SseEmitter subscribe(Long userId) {
        long unread = unreadCount(userId);
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        streamsByUser.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeStream(userId, emitter));
        emitter.onTimeout(() -> removeStream(userId, emitter));
        emitter.onError(error -> removeStream(userId, emitter));
        send(userId, emitter, "unread-count", Map.of("count", unread));
        return emitter;
    }

    int openStreamCount(Long userId) {
        List<SseEmitter> streams = streamsByUser.get(userId);
        return streams == null ? 0 : streams.size();
    }

    private void removeStream(Long userId, SseEmitter emitter) {
        List<SseEmitter> streams = streamsByUser.get(userId);
        if (streams != null) {
            streams.remove(emitter);
            if (streams.isEmpty()) {
                streamsByUser.remove(userId);
            }
        }
    }

    /** Writes one event to every open stream of the user; a stream that fails is dropped. */
    void push(Long userId, String eventName, Object data) {
        List<SseEmitter> streams = streamsByUser.get(userId);
        if (streams == null) {
            return;
        }
        for (SseEmitter emitter : streams) {
            send(userId, emitter, eventName, data);
        }
    }

    private void send(Long userId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException | IllegalStateException closed) {
            removeStream(userId, emitter); // the browser tab was closed
        }
    }

    /** A comment line every 25 seconds keeps idle connections open and finds closed tabs. */
    @Scheduled(fixedRate = 25_000)
    public void keepAlive() {
        for (Map.Entry<Long, List<SseEmitter>> entry : streamsByUser.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().comment("keep-alive"));
                } catch (IOException | IllegalStateException closed) {
                    removeStream(entry.getKey(), emitter);
                }
            }
        }
    }

    // ---------- notifications ----------

    /** Saves a notification and pushes it to the user's open streams once the database commit succeeded. */
    @Transactional
    public Notification create(User user, Product product, String message) {
        Notification saved = notificationRepository.save(new Notification(user, product, message));
        NotificationResponse payload = NotificationResponse.from(saved);
        Long userId = user.getId();
        Runnable deliver = () -> {
            push(userId, "notification", payload);
            log.debug("Notification {} pushed to user {}", payload.id(), userId);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deliver.run();
                }
            });
        } else {
            deliver.run();
        }
        return saved;
    }

    /** FR-31: number for the extension icon. */
    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserAndReadFalse(findUser(userId));
    }

    /** Newest first, at most 50. */
    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Long userId) {
        List<NotificationResponse> result = new ArrayList<>();
        for (Notification notification : notificationRepository.findByUserOrderByCreatedAtDesc(findUser(userId))) {
            if (result.size() == LIST_LIMIT) {
                break;
            }
            result.add(NotificationResponse.from(notification));
        }
        return result;
    }

    /** FR-32. Only the owner can mark a notification as read. */
    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUser(notificationId, findUser(userId))
                .orElseThrow(() -> new ProductNotFoundException("Notification not found"));
        notification.markRead();
        push(userId, "unread-count", Map.of("count", notificationRepository.countByUserAndReadFalse(notification.getUser())));
        return NotificationResponse.from(notification);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(NotLoggedInException::new);
    }
}
