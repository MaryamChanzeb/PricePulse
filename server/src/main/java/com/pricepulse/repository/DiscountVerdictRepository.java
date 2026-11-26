package com.pricepulse.repository;

import com.pricepulse.model.DiscountVerdict;
import com.pricepulse.model.PriceObservation;
import com.pricepulse.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiscountVerdictRepository extends JpaRepository<DiscountVerdict, Long> {

    /** The latest verdict of a product (GET /api/products/{id}/verdict, dashboard cards). */
    Optional<DiscountVerdict> findFirstByProductOrderByCreatedAtDesc(Product product);

    /** The verdict produced by one observation (FR-23). */
    Optional<DiscountVerdict> findByObservation(PriceObservation observation);
}
