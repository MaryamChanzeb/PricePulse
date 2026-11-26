package com.pricepulse.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WatchItemTest {

    private Product product;
    private User shopper;

    @BeforeEach
    void setUp() {
        Store daraz = new Store("Daraz", "daraz.pk", "PKR", true);
        product = new Product(daraz, "123", "Air Fryer 5L", "https://www.daraz.pk/products/a-i123.html", null);
        shopper = new Shopper("Sara", "sara@mail.pk", "hash");
    }

    @Test
    void newItemIsArmed() {
        WatchItem item = new WatchItem(shopper, product, new BigDecimal("9500"));
        assertThat(item.isAlertArmed()).isTrue();
        assertThat(item.getTargetPrice()).isEqualByComparingTo("9500");
    }

    @Test
    void disarmAndArmAgain() {
        WatchItem item = new WatchItem(shopper, product, new BigDecimal("9500"));
        item.disarm();
        assertThat(item.isAlertArmed()).isFalse();
        item.arm();
        assertThat(item.isAlertArmed()).isTrue();
    }

    @Test
    void changingTheTargetArmsTheItemAgain() {
        WatchItem item = new WatchItem(shopper, product, new BigDecimal("9500"));
        item.disarm();
        item.changeTarget(new BigDecimal("9000"));
        assertThat(item.getTargetPrice()).isEqualByComparingTo("9000");
        assertThat(item.isAlertArmed()).isTrue();
    }

    @Test
    void targetMustBeGreaterThanZero() {
        assertThatThrownBy(() -> new WatchItem(shopper, product, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        WatchItem item = new WatchItem(shopper, product, new BigDecimal("9500"));
        assertThatThrownBy(() -> item.changeTarget(new BigDecimal("-5")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(item.getTargetPrice()).isEqualByComparingTo("9500");
    }
}
