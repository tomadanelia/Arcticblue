package com.arcticblue.algorithm;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class TradeOptimizationAlgorithm {

    public OptimizationResult optimize(long maxMargin, List<TradeCandidate> trades) {
        Map<Long, State> frontier = new TreeMap<>();
        frontier.put(0L, new State(0L, null));

        for (int index = 0; index < trades.size(); index++) {
            TradeCandidate trade = trades.get(index);
            Map<Long, State> candidates = new HashMap<>(frontier);

            for (Map.Entry<Long, State> entry : frontier.entrySet()) {
                long margin = safeAdd(entry.getKey(), trade.marginRequired(), "margin total is too large");
                if (margin > maxMargin) {
                    continue;
                }
                long pnl = safeAdd(entry.getValue().pnl(), trade.expectedPnl(), "P&L total is too large");
                State current = candidates.get(margin);
                if (current == null || pnl > current.pnl()) {
                    candidates.put(margin, new State(pnl, new Selection(index, entry.getValue().selection())));
                }
            }
            frontier = removeDominated(candidates);
        }

        Map.Entry<Long, State> best = frontier.entrySet().stream()
                .max((left, right) -> {
                    int pnlComparison = Long.compare(left.getValue().pnl(), right.getValue().pnl());
                    return pnlComparison != 0 ? pnlComparison : Long.compare(right.getKey(), left.getKey());
                })
                .orElseThrow();

        List<Integer> selected = new ArrayList<>();
        for (Selection node = best.getValue().selection(); node != null; node = node.previous()) {
            selected.add(node.tradeIndex());
        }
        Collections.reverse(selected);
        return new OptimizationResult(List.copyOf(selected), best.getKey(), best.getValue().pnl());
    }

    private Map<Long, State> removeDominated(Map<Long, State> states) {
        Map<Long, State> result = new TreeMap<>();
        long bestPnl = Long.MIN_VALUE;
        for (Map.Entry<Long, State> entry : new TreeMap<>(states).entrySet()) {
            if (entry.getValue().pnl() > bestPnl) {
                result.put(entry.getKey(), entry.getValue());
                bestPnl = entry.getValue().pnl();
            }
        }
        return result;
    }

    private long safeAdd(long left, long right, String message) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(message, exception);
        }
    }

    private record State(long pnl, Selection selection) {
    }

    private record Selection(int tradeIndex, Selection previous) {
    }
}
