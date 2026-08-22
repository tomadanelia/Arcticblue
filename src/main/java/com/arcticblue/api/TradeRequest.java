package com.arcticblue.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TradeRequest(
        @NotBlank @Size(max = 200) String tradeName,
        @NotNull @Positive Long marginRequired,
        @NotNull Long expectedPnl) {
}
