package com.arcticblue.api;

public record TradeResponse(String tradeName, long marginRequired, long expectedPnl) {
}
