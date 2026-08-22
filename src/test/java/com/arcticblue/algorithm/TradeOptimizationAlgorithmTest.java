package com.arcticblue.algorithm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TradeOptimizationAlgorithmTest {

    private final TradeOptimizationAlgorithm algorithm = new TradeOptimizationAlgorithm();

    @Test
    void selectsCombinationWithMaximumPnl() {
        List<TradeCandidate> trades = List.of(
                new TradeCandidate("Trade Alpha", 5, 120),
                new TradeCandidate("Trade Beta", 10, 200),
                new TradeCandidate("Trade Gamma", 3, 80),
                new TradeCandidate("Trade Delta", 8, 160));

        OptimizationResult result = algorithm.optimize(15, trades);

        assertThat(result.selectedIndices()).containsExactly(0, 1);
        assertThat(result.totalMargin()).isEqualTo(15);
        assertThat(result.totalPnl()).isEqualTo(320);
    }

    @Test
    void returnsEmptySelectionWhenNothingFits() {
        OptimizationResult result = algorithm.optimize(2, List.of(
                new TradeCandidate("Large trade", 3, 100)));

        assertThat(result.selectedIndices()).isEmpty();
        assertThat(result.totalMargin()).isZero();
        assertThat(result.totalPnl()).isZero();
    }

    @Test
    void doesNotSelectNegativePnlTrades() {
        OptimizationResult result = algorithm.optimize(10, List.of(
                new TradeCandidate("Loss", 1, -10),
                new TradeCandidate("Profit", 5, 25)));

        assertThat(result.selectedIndices()).containsExactly(1);
        assertThat(result.totalPnl()).isEqualTo(25);
    }

    @Test
    void choosesLowerMarginWhenPnlIsTied() {
        OptimizationResult result = algorithm.optimize(10, List.of(
                new TradeCandidate("Heavy", 10, 100),
                new TradeCandidate("Light", 5, 100)));

        assertThat(result.selectedIndices()).containsExactly(1);
        assertThat(result.totalMargin()).isEqualTo(5);
    }
}
