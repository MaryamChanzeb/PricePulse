package com.pricepulse.repository;

import com.pricepulse.model.Product;
import com.pricepulse.model.User;
import com.pricepulse.model.WatchItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchItemRepository extends JpaRepository<WatchItem, Long> {

    /** A shopper's watchlist (FR-33). */
    List<WatchItem> findByUserOrderByCreatedAtAsc(User user);

    /** Everyone watching a product: checked after each accepted observation (FR-28, FR-29). */
    List<WatchItem> findByProduct(Product product);

    /** FR-25 duplicate check. */
    boolean existsByUserAndProduct(User user, Product product);

    /** FR-26 limit check. */
    long countByUser(User user);

    /** An item only its owner may change or delete (FR-27). */
    Optional<WatchItem> findByIdAndUser(Long id, User user);
}
