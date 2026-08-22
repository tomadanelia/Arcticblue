package com.arcticblue.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OptimizationResponse(
        UUID requestId,
        List<TradeResponse> selectedTrades,
        long totalMarginRequired,
        long totalExpectedPnl,
        Instant createdAt) {
}
