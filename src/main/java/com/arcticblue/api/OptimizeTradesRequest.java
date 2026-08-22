package com.arcticblue.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OptimizeTradesRequest(
        @NotNull @PositiveOrZero Long maxMargin,
        @NotNull @Size(max = 1000) List<@Valid TradeRequest> candidateTrades) {
}
