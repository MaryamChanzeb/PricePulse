package com.pricepulse.service;

import com.pricepulse.TestRules;
import com.pricepulse.TestUsers;
import com.pricepulse.exception.DuplicateWatchException;
import com.pricepulse.exception.ProductNotFoundException;
import com.pricepulse.exception.ValidationException;
import com.pricepulse.exception.WatchLimitExceededException;
import com.pricepulse.model.Product;
import com.pricepulse.model.Shopper;
import com.pricepulse.model.Store;
import com.pricepulse.model.WatchItem;
import com.pricepulse.repository.DiscountVerdictRepository;
import com.pricepulse.repository.PriceObservationRepository;
import com.pricepulse.repository.ProductRepository;
import com.pricepulse.repository.UserRepository;
import com.pricepulse.repository.WatchItemRepository;
import com.pricepulse.rules.PercentageDropAlertRule;
import com.pricepulse.rules.TargetPriceAlertRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FR-24 to FR-29 with mocked repositories (no database). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WatchlistServiceTest {

    @Mock private WatchItemRepository watchItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private PriceObservationRepository observationRepository;
    @Mock private DiscountVerdictRepository verdictRepository;
    @Mock private NotificationService notificationService;

    private WatchlistService service;
    private Shopper sara;
    private Product watch;

    @BeforeEach
    void setUp() {
        service = new WatchlistService(watchItemRepository, productRepository, userRepository, observationRepository,
                verdictRepository, List.of(new TargetPriceAlertRule(), new PercentageDropAlertRule(TestRules.defaults())),
                notificationService, TestRules.defaults());
        sara = TestUsers.shopper(1L, "sara@mail.pk");
        watch = new Product(new Store("Daraz", "daraz.pk", "PKR", true), "123", "Smart Watch X2",
                "https://www.daraz.pk/products/w-i123.html", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sara));
        when(productRepository.findById(10L)).thenReturn(Optional.of(watch));
        when(watchItemRepository.saveAndFlush(any(WatchItem.class))).thenAnswer(call -> call.getArgument(0));
        when(observationRepository.findFirstByProductAndStatusOrderByObservedAtDesc(any(), any())).thenReturn(Optional.empty());
        when(verdictRepository.findFirstByProductOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
    }

    private static BigDecimal rs(String value) {
        return new BigDecimal(value);
    }

    // ---------- adding (FR-24 to FR-26) ----------

    @Test
    void addsAnArmedItem() {
        assertThat(service.add(1L, 10L, rs("5000")).alertArmed()).isTrue();
    }

    @Test
    void sameProductTwiceIsRefused() {
        when(watchItemRepository.existsByUserAndProduct(sara, watch)).thenReturn(true);
        assertThatThrownBy(() -> service.add(1L, 10L, rs("5000")))
                .isInstanceOf(DuplicateWatchException.class).hasMessage("Already in your watchlist");
    }

    @Test
    void twentyFirstItemIsRefusedButTwentiethIsAllowed() {
        when(watchItemRepository.countByUser(sara)).thenReturn(19L);
        assertThat(service.add(1L, 10L, rs("5000"))).isNotNull();

        when(watchItemRepository.countByUser(sara)).thenReturn(20L);
        assertThatThrownBy(() -> service.add(1L, 10L, rs("5000")))
                .isInstanceOf(WatchLimitExceededException.class).hasMessage("Watchlist limit reached (20)");
    }

    @Test
    void targetMustBeGreaterThanZeroAndProductMustExist() {
        assertThatThrownBy(() -> service.add(1L, 10L, BigDecimal.ZERO))
                .isInstanceOf(ValidationException.class).hasMessage("Target price must be greater than zero");
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.add(1L, 99L, rs("5000"))).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void someoneElsesItemCannotBeChanged() {
        when(watchItemRepository.findByIdAndUser(5L, sara)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.changeTarget(1L, 5L, rs("4000")))
                .isInstanceOf(ProductNotFoundException.class).hasMessage("Watch item not found");
    }

    // ---------- alerts (FR-28, FR-29) ----------

    private WatchItem watching(String target) {
        WatchItem item = new WatchItem(sara, watch, rs(target));
        when(watchItemRepository.findByProduct(watch)).thenReturn(List.of(item));
        return item;
    }

    @Test
    void priceAtTargetSendsOneAlertAndDisarms() {
        WatchItem item = watching("5000");

        assertThat(service.checkAlerts(watch, rs("5500"), rs("5000"))).isEqualTo(1);
        verify(notificationService).create(eq(sara), eq(watch), contains("at or below your target"));
        assertThat(item.isAlertArmed()).isFalse();
    }

    @Test
    void secondLowPriceDoesNotAlertAgain() {
        WatchItem item = watching("5000");
        service.checkAlerts(watch, rs("5500"), rs("4900"));
        service.checkAlerts(watch, rs("4900"), rs("4800")); // still below target: same drop, no new alert

        verify(notificationService, org.mockito.Mockito.times(1)).create(any(), any(), any());
        assertThat(item.isAlertArmed()).isFalse();
    }

    @Test
    void priceBackAboveTargetReArmsAndTheNextDropAlertsAgain() {
        WatchItem item = watching("5000");
        service.checkAlerts(watch, rs("5500"), rs("4900"));   // alert 1, disarmed
        service.checkAlerts(watch, rs("4900"), rs("5600"));   // back above: re-armed, no alert
        assertThat(item.isAlertArmed()).isTrue();
        service.checkAlerts(watch, rs("5600"), rs("4950"));   // alert 2

        verify(notificationService, org.mockito.Mockito.times(2)).create(any(), any(), any());
    }

    @Test
    void bigDropAboveTheTargetAlsoAlerts() {
        WatchItem item = watching("3000");
        assertThat(service.checkAlerts(watch, rs("8000"), rs("6000"))).isEqualTo(1); // -25%, still above 3000
        verify(notificationService).create(eq(sara), eq(watch), contains("dropped 25%"));
        assertThat(item.isAlertArmed()).isFalse();
    }

    @Test
    void smallChangeAboveTargetDoesNothing() {
        WatchItem item = watching("3000");
        assertThat(service.checkAlerts(watch, rs("8000"), rs("7900"))).isZero();
        verify(notificationService, never()).create(any(), any(), any());
        assertThat(item.isAlertArmed()).isTrue();
    }
}
