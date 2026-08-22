package com.arcticblue.service;

import java.util.UUID;

public class OptimizationNotFoundException extends RuntimeException {
    public OptimizationNotFoundException(UUID requestId) {
        super("Optimization request not found: " + requestId);
    }
}
