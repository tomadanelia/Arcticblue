package com.arcticblue.api;

import java.util.List;

public record PagedOptimizationResponse(
        List<OptimizationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
