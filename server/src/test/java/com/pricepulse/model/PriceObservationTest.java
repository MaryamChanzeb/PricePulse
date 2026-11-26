package com.pricepulse.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceObservationTest {

    private Product product;
    private User shopper;

    @BeforeEach
    void setUp() {
        Store daraz = new Store("Daraz", "daraz.pk", "PKR", true);
        product = new Product(daraz, "123", "Smart Watch X2", "https://www.daraz.pk/products/x-i123.html", null);
        shopper = new Shopper("Sara", "sara@mail.pk", "hash");
    }

    private PriceObservation newObservation() {
        return new PriceObservation(product, shopper, new BigDecimal("5999.00"), new BigDecimal("9999.00"), true, Instant.now());
    }

    @Test
    void newObservationIsAccepted() {
        PriceObservation observation = newObservation();
        assertThat(observation.getStatus()).isEqualTo(ObservationStatus.ACCEPTED);
        assertThat(observation.isAccepted()).isTrue();
        assertThat(observation.hasClaimedOriginalPrice()).isTrue();
    }

    @Test
    void flaggedObservationCanBeApproved() {
        PriceObservation observation = newObservation();
        observation.markFlagged();
        assertThat(observation.getStatus()).isEqualTo(ObservationStatus.FLAGGED);
        assertThat(observation.isAccepted()).isFalse();

        observation.approve();
        assertThat(observation.getStatus()).isEqualTo(ObservationStatus.ACCEPTED);
    }

    @Test
    void flaggedObservationCanBeRejectedAndThenNeverApproved() {
        PriceObservation observation = newObservation();
        observation.markFlagged();
        observation.reject();
        assertThat(observation.getStatus()).isEqualTo(ObservationStatus.REJECTED);

        assertThatThrownBy(observation::approve).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptedObservationCannotBeApprovedOrRejected() {
        PriceObservation observation = newObservation();
        assertThatThrownBy(observation::approve).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(observation::reject).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void zeroOrNegativePriceIsImpossible() {
        assertThatThrownBy(() -> new PriceObservation(product, shopper, BigDecimal.ZERO, null, false, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PriceObservation(product, shopper, new BigDecimal("-1"), null, false, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
