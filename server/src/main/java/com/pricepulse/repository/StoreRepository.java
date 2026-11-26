package com.pricepulse.repository;

import com.pricepulse.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    /** Which store does an observed page belong to? (FR-11) */
    Optional<Store> findByDomain(String domain);

    boolean existsByDomain(String domain);

    /** Admin store list (FR-38). */
    List<Store> findAllByOrderByNameAsc();
}
