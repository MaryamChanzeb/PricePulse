package com.pricepulse.service;

import com.pricepulse.exception.ProductNotFoundException;
import com.pricepulse.exception.ValidationException;
import com.pricepulse.model.Verdict;
import com.pricepulse.model.Product;
import com.pricepulse.model.Store;
import com.pricepulse.repository.DiscountVerdictRepository;
import com.pricepulse.repository.PriceObservationRepository;
import com.pricepulse.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private PriceObservationRepository observationRepository;
    @Mock private DiscountVerdictRepository verdictRepository;
    @InjectMocks private ProductService productService;

    @Test
    void historyOnlyFor30Or90Days() {
        assertThatThrownBy(() -> productService.history(1L, 45))
                .isInstanceOf(ValidationException.class).hasMessage("days must be 30 or 90");
    }

    @Test
    void unknownProductGives404Exception() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.latestVerdict(99L))
                .isInstanceOf(ProductNotFoundException.class).hasMessage("Product not found");
    }

    @Test
    void productWithoutVerdictSaysNotAnalysedYet() {
        Product product = new Product(new Store("Daraz", "daraz.pk", "PKR", true), "1", "Lamp",
                "https://www.daraz.pk/products/l-i1.html", null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(verdictRepository.findFirstByProductOrderByCreatedAtDesc(product)).thenReturn(Optional.empty());

        assertThat(productService.latestVerdict(1L).verdict()).isEqualTo(Verdict.NOT_ENOUGH_DATA);
    }
}
