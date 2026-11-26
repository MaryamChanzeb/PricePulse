package com.pricepulse.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StoreAndNotificationTest {

    @Test
    void storeDomainIsStoredInLowerCaseAndCanBeSwitchedOffAndOn() {
        Store store = new Store("Daraz", "DARAZ.PK", "pkr", true);
        assertThat(store.getDomain()).isEqualTo("daraz.pk");
        assertThat(store.getCurrency()).isEqualTo("PKR");

        store.disable();
        assertThat(store.isSupported()).isFalse();
        store.enable();
        assertThat(store.isSupported()).isTrue();
    }

    @Test
    void notificationStartsUnreadAndCanBeMarkedRead() {
        Store store = new Store("Daraz", "daraz.pk", "PKR", true);
        Product product = new Product(store, "1", "Backpack", "https://www.daraz.pk/products/b-i1.html", " ");
        Notification notification = new Notification(new Shopper("Sara", "s@mail.pk", "hash"), product, "Price dropped");

        assertThat(product.getImageUrl()).isNull();
        assertThat(notification.isRead()).isFalse();
        notification.markRead();
        assertThat(notification.isRead()).isTrue();
    }
}
