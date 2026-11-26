package com.pricepulse.repository;

import com.pricepulse.model.ObservationStatus;
import com.pricepulse.model.PriceObservation;
import com.pricepulse.model.Product;
import com.pricepulse.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PriceObservationRepository extends JpaRepository<PriceObservation, Long> {

    /** "History" for the rules: accepted observations of a product, newest first (SDS 4.1). */
    List<PriceObservation> findByProductAndStatusOrderByObservedAtDesc(Product product, ObservationStatus status);

    /** Price history chart for the last 30 or 90 days, oldest first (FR-34). */
    List<PriceObservation> findByProductAndStatusAndObservedAtAfterOrderByObservedAtAsc(
            Product product, ObservationStatus status, Instant after);

    /** The last accepted price, used for the outlier check (FR-13). */
    Optional<PriceObservation> findFirstByProductAndStatusOrderByObservedAtDesc(Product product, ObservationStatus status);

    /** Same user + same product inside the duplicate window? (FR-12) */
    Optional<PriceObservation> findFirstByProductAndUserAndObservedAtAfterOrderByObservedAtDesc(
            Product product, User user, Instant after);

    /** Admin review list (FR-36), oldest first. */
    List<PriceObservation> findByStatusOrderByObservedAtAsc(ObservationStatus status);

    long countByProductAndStatus(Product product, ObservationStatus status);
}
