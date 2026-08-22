package com.arcticblue.algorithm;

import java.util.List;

public record OptimizationResult(List<Integer> selectedIndices, long totalMargin, long totalPnl) {
}
