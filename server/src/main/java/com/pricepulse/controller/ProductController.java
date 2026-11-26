package com.pricepulse.controller;

import com.pricepulse.dto.ProductHistoryResponse;
import com.pricepulse.dto.VerdictResponse;
import com.pricepulse.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/products/{id}/verdict and GET /api/products/{id}/history?days= (SDS 6). Shopper role. */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}/verdict")
    public VerdictResponse verdict(@PathVariable("id") Long productId) {
        return productService.latestVerdict(productId);
    }

    @GetMapping("/{id}/history")
    public ProductHistoryResponse history(@PathVariable("id") Long productId,
                                          @RequestParam(name = "days", defaultValue = "30") int days) {
        return productService.history(productId, days);
    }
}
