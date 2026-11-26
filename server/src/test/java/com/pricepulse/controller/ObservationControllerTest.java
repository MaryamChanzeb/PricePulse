package com.pricepulse.controller;

import com.pricepulse.TestRules;
import com.pricepulse.TestUsers;
import com.pricepulse.dto.ObservationRequest;
import com.pricepulse.dto.ObservationResponse;
import com.pricepulse.exception.PriceValidationException;
import com.pricepulse.exception.UnsupportedStoreException;
import com.pricepulse.exception.ValidationException;
import com.pricepulse.model.DiscountVerdict;
import com.pricepulse.model.ObservationStatus;
import com.pricepulse.model.Verdict;
import com.pricepulse.repository.DiscountVerdictRepository;
import com.pricepulse.service.DiscountAnalysisService;
import com.pricepulse.service.WatchlistService;
import com.pricepulse.model.PriceObservation;
import com.pricepulse.model.Product;
import com.pricepulse.model.Role;
import com.pricepulse.model.Shopper;
import com.pricepulse.model.Store;
import com.pricepulse.repository.PriceObservationRepository;
import com.pricepulse.repository.ProductRepository;
import com.pricepulse.repository.StoreRepository;
import com.pricepulse.repository.UserRepository;
import com.pricepulse.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the FIRST version of POST /api/observations (FR-10 to FR-14).
 * The same behaviour must still pass after the refactoring in slice Z8.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ObservationControllerTest {

    private static final String DARAZ_URL = "https://www.daraz.pk/products/smart-watch-x2-i123.html";

    @Mock private UserRepository userRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PriceObservationRepository observationRepository;
    @Mock private DiscountVerdictRepository verdictRepository;
    @Mock private DiscountAnalysisService discountAnalysisService;
    @Mock private WatchlistService watchlistService;

    private ObservationController controller;
    private final AuthenticatedUser caller = new AuthenticatedUser(1L, "sara@mail.pk", Role.SHOPPER);
    private Shopper sara;
    private Store daraz;
    private Product watch;

    @BeforeEach
    void setUp() {
        controller = new ObservationController(userRepository, storeRepository, productRepository,
                observationRepository, verdictRepository, discountAnalysisService, watchlistService, TestRules.defaults());
        sara = TestUsers.shopper(1L, "sara@mail.pk");
        daraz = new Store("Daraz", "daraz.pk", "PKR", true);
        watch = new Product(daraz, "123", "Smart Watch X2", DARAZ_URL, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sara));
        when(storeRepository.findByDomain("daraz.pk")).thenReturn(Optional.of(daraz));
        when(productRepository.findByStoreAndStoreSku(daraz, "123")).thenReturn(Optional.of(watch));
        when(observationRepository.findFirstByProductAndUserAndObservedAtAfterOrderByObservedAtDesc(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(observationRepository.findFirstByProductAndStatusOrderByObservedAtDesc(any(), eq(ObservationStatus.ACCEPTED)))
                .thenReturn(Optional.empty());
        when(observationRepository.save(any(PriceObservation.class))).thenAnswer(call -> call.getArgument(0));
        when(verdictRepository.findFirstByProductOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
        when(discountAnalysisService.analyse(any(PriceObservation.class))).thenAnswer(call -> {
            PriceObservation observation = call.getArgument(0);
            return new DiscountVerdict(observation.getProduct(), observation, Verdict.GENUINE, null, null, "test");
        });
    }

    @Test
    void acceptedPriceIsAnalysedButFlaggedPriceIsNot() {
        ObservationResponse accepted = controller.submit(caller, request("5999", null));
        assertThat(accepted.verdict().verdict()).isEqualTo(Verdict.GENUINE);

        lastAcceptedPriceIs("5999");
        ObservationResponse flagged = controller.submit(caller, request("599", null));
        assertThat(flagged.verdict().verdict()).isEqualTo(Verdict.NOT_ENOUGH_DATA); // no earlier verdict exists
        verify(discountAnalysisService, org.mockito.Mockito.times(1)).analyse(any());
    }

    @Test
    void alertsAreCheckedWithThePreviousPriceOnlyForAcceptedPrices() {
        lastAcceptedPriceIs("6000");
        controller.submit(caller, request("5000", null));               // -16.7%: accepted
        verify(watchlistService).checkAlerts(eq(watch), argThat(p -> p.compareTo(new BigDecimal("6000")) == 0),
                argThat(p -> p.compareTo(new BigDecimal("5000")) == 0));

        controller.submit(caller, request("100", null));                // -98%: flagged, no alerts
        verify(watchlistService, org.mockito.Mockito.times(1)).checkAlerts(any(), any(), any());
    }

    private ObservationRequest request(String price, String claimed) {
        return new ObservationRequest(DARAZ_URL, "123", "Smart Watch X2", null, price, claimed, true);
    }

    private void lastAcceptedPriceIs(String price) {
        PriceObservation last = new PriceObservation(watch, sara, new BigDecimal(price), null, false,
                Instant.now().minusSeconds(86400));
        when(observationRepository.findFirstByProductAndStatusOrderByObservedAtDesc(watch, ObservationStatus.ACCEPTED))
                .thenReturn(Optional.of(last));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "abc", "NaN", "10000000.01", ""})
    void invalidPricesAreRejected(String price) {
        assertThatThrownBy(() -> controller.submit(caller, request(price, null)))
                .isInstanceOf(PriceValidationException.class)
                .hasMessage("Invalid price");
        verify(observationRepository, never()).save(any());
    }

    @Test
    void maximumPriceItselfIsAccepted() {
        ObservationResponse response = controller.submit(caller, request("10000000", null));
        assertThat(response.status()).isEqualTo(ObservationStatus.ACCEPTED);
    }

    @Test
    void unsupportedOrDisabledStoreIsRejected() {
        ObservationRequest other = new ObservationRequest("https://www.example.pk/item/1", "1", "Item", null, "100", null, false);
        assertThatThrownBy(() -> controller.submit(caller, other))
                .isInstanceOf(UnsupportedStoreException.class)
                .hasMessage("This store is not supported");

        daraz.disable();
        assertThatThrownBy(() -> controller.submit(caller, request("5999", null)))
                .isInstanceOf(UnsupportedStoreException.class);
    }

    @Test
    void missingSkuIsRejected() {
        ObservationRequest noSku = new ObservationRequest(DARAZ_URL, " ", "Smart Watch X2", null, "5999", null, false);
        assertThatThrownBy(() -> controller.submit(caller, noSku))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product SKU is required");
    }

    @Test
    void newSkuCreatesTheProduct() {
        when(productRepository.findByStoreAndStoreSku(daraz, "999")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));
        ObservationRequest newProduct = new ObservationRequest(DARAZ_URL, "999", "New Item", null, "1500", null, false);

        controller.submit(caller, newProduct);

        ArgumentCaptor<Product> created = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(created.capture());
        assertThat(created.getValue().getStoreSku()).isEqualTo("999");
        assertThat(created.getValue().getStore()).isSameAs(daraz);
    }

    @Test
    void secondObservationWithinSixHoursIsIgnored() {
        PriceObservation earlier = new PriceObservation(watch, sara, new BigDecimal("5999"), null, true,
                Instant.now().minusSeconds(3600));
        when(observationRepository.findFirstByProductAndUserAndObservedAtAfterOrderByObservedAtDesc(eq(watch), eq(sara), any()))
                .thenReturn(Optional.of(earlier));

        ObservationResponse response = controller.submit(caller, request("5999", "9999"));

        assertThat(response.duplicate()).isTrue();
        verify(observationRepository, never()).save(any());
    }

    @Test
    void changeOfMoreThanSixtyPercentIsFlagged() {
        lastAcceptedPriceIs("5999");
        ObservationResponse response = controller.submit(caller, request("599", null)); // -90%
        assertThat(response.status()).isEqualTo(ObservationStatus.FLAGGED);
    }

    @Test
    void changeOfExactlySixtyPercentIsStillAccepted() {
        lastAcceptedPriceIs("1000");
        assertThat(controller.submit(caller, request("1600", null)).status()).isEqualTo(ObservationStatus.ACCEPTED);
        assertThat(controller.submit(caller, request("400", null)).status()).isEqualTo(ObservationStatus.ACCEPTED);
        assertThat(controller.submit(caller, request("1600.01", null)).status()).isEqualTo(ObservationStatus.FLAGGED);
    }

    @Test
    void claimedPriceNotAboveThePriceIsIgnored() {
        controller.submit(caller, request("5999", "5000"));

        ArgumentCaptor<PriceObservation> saved = ArgumentCaptor.forClass(PriceObservation.class);
        verify(observationRepository).save(saved.capture());
        assertThat(saved.getValue().getClaimedOriginalPrice()).isNull();
    }

    @Test
    void firstObservationOfAProductIsAccepted() {
        ObservationResponse response = controller.submit(caller, request("5999", "9999"));
        assertThat(response.status()).isEqualTo(ObservationStatus.ACCEPTED);
        assertThat(response.duplicate()).isFalse();
    }
}
