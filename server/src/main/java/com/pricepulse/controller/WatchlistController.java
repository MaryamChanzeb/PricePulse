package com.pricepulse.controller;

import com.pricepulse.dto.TargetRequest;
import com.pricepulse.dto.WatchItemResponse;
import com.pricepulse.dto.WatchRequest;
import com.pricepulse.security.AuthenticatedUser;
import com.pricepulse.service.WatchlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** POST, GET, PUT and DELETE /api/watchlist (SDS 6). Shopper role. */
@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchItemResponse add(@AuthenticationPrincipal AuthenticatedUser caller, @Valid @RequestBody WatchRequest request) {
        return watchlistService.add(caller.id(), request.productId(), request.targetPrice());
    }

    @GetMapping
    public List<WatchItemResponse> list(@AuthenticationPrincipal AuthenticatedUser caller) {
        return watchlistService.list(caller.id());
    }

    @PutMapping("/{id}")
    public WatchItemResponse changeTarget(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable("id") Long itemId,
                                          @Valid @RequestBody TargetRequest request) {
        return watchlistService.changeTarget(caller.id(), itemId, request.targetPrice());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable("id") Long itemId) {
        watchlistService.remove(caller.id(), itemId);
    }
}
