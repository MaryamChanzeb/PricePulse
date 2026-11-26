package com.pricepulse.service;

import com.pricepulse.TestUsers;
import com.pricepulse.exception.ProductNotFoundException;
import com.pricepulse.model.Notification;
import com.pricepulse.model.Product;
import com.pricepulse.model.Shopper;
import com.pricepulse.model.Store;
import com.pricepulse.repository.NotificationRepository;
import com.pricepulse.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;

    private NotificationService service;
    private Shopper sara;
    private Product watch;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, userRepository);
        sara = TestUsers.shopper(1L, "sara@mail.pk");
        watch = new Product(new Store("Daraz", "daraz.pk", "PKR", true), "123", "Smart Watch X2",
                "https://www.daraz.pk/products/w-i123.html", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sara));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void eachOpenTabGetsItsOwnStreamInTheMap() {
        service.subscribe(1L);
        service.subscribe(1L);
        assertThat(service.openStreamCount(1L)).isEqualTo(2);
        assertThat(service.openStreamCount(2L)).isZero();
    }

    @Test
    void createSavesAnUnreadNotification() {
        service.subscribe(1L);
        Notification saved = service.create(sara, watch, "Smart Watch X2 is now Rs 4,999");

        verify(notificationRepository).save(any(Notification.class));
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getMessage()).contains("Rs 4,999");
    }

    @Test
    void someoneElsesNotificationCannotBeMarkedRead() {
        when(notificationRepository.findByIdAndUser(7L, sara)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markRead(1L, 7L))
                .isInstanceOf(ProductNotFoundException.class).hasMessage("Notification not found");
    }

    @Test
    void markReadChangesTheNotification() {
        Notification notification = new Notification(sara, watch, "Price dropped");
        when(notificationRepository.findByIdAndUser(7L, sara)).thenReturn(Optional.of(notification));

        assertThat(service.markRead(1L, 7L).read()).isTrue();
        assertThat(notification.isRead()).isTrue();
    }
}
