package com.pricepulse.repository;

import com.pricepulse.model.Product;
import com.pricepulse.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Has this store's SKU been seen before? If not, the product is created (FR-14). */
    Optional<Product> findByStoreAndStoreSku(Store store, String storeSku);
}
