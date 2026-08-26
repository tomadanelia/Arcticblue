package com.arcticblue.api;

import com.arcticblue.service.TradeOptimizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/trades")
public class TradeOptimizationController {

    private final TradeOptimizationService service;

    public TradeOptimizationController(TradeOptimizationService service) {
        this.service = service;
    }

    @PostMapping("/optimize")
    public ResponseEntity<OptimizationResponse> optimize(@Valid @RequestBody OptimizeTradesRequest request) {
        OptimizationResponse response = service.optimize(request);
        if (response.selectedTrades().isEmpty()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{requestId}")
    public OptimizationResponse get(@PathVariable UUID requestId) {
        return service.get(requestId);
    }

    @GetMapping
    public PagedOptimizationResponse getAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.getAll(page, size);
    }
}
