package com.pricepulse.service;

import com.pricepulse.dto.ProductHistoryResponse;
import com.pricepulse.dto.VerdictResponse;
import com.pricepulse.exception.ProductNotFoundException;
import com.pricepulse.exception.ValidationException;
import com.pricepulse.model.ObservationStatus;
import com.pricepulse.model.PriceObservation;
import com.pricepulse.model.Product;
import com.pricepulse.repository.DiscountVerdictRepository;
import com.pricepulse.repository.PriceObservationRepository;
import com.pricepulse.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Read-only product information for the dashboard and the popup (FR-21, FR-34). */
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final PriceObservationRepository observationRepository;
    private final DiscountVerdictRepository verdictRepository;

    public ProductService(ProductRepository productRepository, PriceObservationRepository observationRepository,
                          DiscountVerdictRepository verdictRepository) {
        this.productRepository = productRepository;
        this.observationRepository = observationRepository;
        this.verdictRepository = verdictRepository;
    }

    /** GET /api/products/{id}/verdict */
    public VerdictResponse latestVerdict(Long productId) {
        Product product = find(productId);
        return verdictRepository.findFirstByProductOrderByCreatedAtDesc(product)
                .map(VerdictResponse::from)
                .orElse(VerdictResponse.notAnalysedYet(product.getId()));
    }

    /** GET /api/products/{id}/history?days=30 or 90 (FR-34: accepted observations only). */
    public ProductHistoryResponse history(Long productId, int days) {
        if (days != 30 && days != 90) {
            throw new ValidationException("days must be 30 or 90");
        }
        Product product = find(productId);
        Instant from = Instant.now().minus(Duration.ofDays(days));
        List<PriceObservation> accepted = observationRepository
                .findByProductAndStatusAndObservedAtAfterOrderByObservedAtAsc(product, ObservationStatus.ACCEPTED, from);

        List<ProductHistoryResponse.Point> points = new ArrayList<>();
        for (PriceObservation observation : accepted) {
            points.add(new ProductHistoryResponse.Point(observation.getPrice(), observation.getClaimedOriginalPrice(),
                    observation.isDiscountBadgeShown(), observation.getObservedAt()));
        }
        long acceptedCount = observationRepository.countByProductAndStatus(product, ObservationStatus.ACCEPTED);
        long flaggedCount = observationRepository.countByProductAndStatus(product, ObservationStatus.FLAGGED);
        Instant lastSeen = observationRepository
                .findFirstByProductAndStatusOrderByObservedAtDesc(product, ObservationStatus.ACCEPTED)
                .map(PriceObservation::getObservedAt)
                .orElse(null);

        return new ProductHistoryResponse(product.getId(), product.getTitle(), product.getStore().getName(),
                product.getUrl(), product.getImageUrl(), days, points, acceptedCount, flaggedCount, lastSeen);
    }

    private Product find(Long productId) {
        return productRepository.findById(productId).orElseThrow(ProductNotFoundException::new);
    }
}
