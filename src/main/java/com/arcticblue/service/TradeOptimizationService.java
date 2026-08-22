package com.arcticblue.service;

import com.arcticblue.algorithm.OptimizationResult;
import com.arcticblue.algorithm.TradeCandidate;
import com.arcticblue.algorithm.TradeOptimizationAlgorithm;
import com.arcticblue.api.OptimizationResponse;
import com.arcticblue.api.OptimizeTradesRequest;
import com.arcticblue.api.PagedOptimizationResponse;
import com.arcticblue.api.TradeRequest;
import com.arcticblue.api.TradeResponse;
import com.arcticblue.persistence.OptimizationRunEntity;
import com.arcticblue.persistence.OptimizationRunRepository;
import com.arcticblue.persistence.OptimizationTradeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TradeOptimizationService {

    private final TradeOptimizationAlgorithm algorithm;
    private final OptimizationRunRepository repository;
    private final Clock clock;

    public TradeOptimizationService(TradeOptimizationAlgorithm algorithm, OptimizationRunRepository repository, Clock clock) {
        this.algorithm = algorithm;
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public OptimizationResponse optimize(OptimizeTradesRequest request) {
        List<TradeCandidate> candidates = request.candidateTrades().stream()
                .map(trade -> new TradeCandidate(trade.tradeName(), trade.marginRequired(), trade.expectedPnl()))
                .toList();
        OptimizationResult result = algorithm.optimize(request.maxMargin(), candidates);
        Set<Integer> selectedIndices = new HashSet<>(result.selectedIndices());

        OptimizationRunEntity run = new OptimizationRunEntity(
                UUID.randomUUID(), request.maxMargin(), result.totalMargin(), result.totalPnl(), Instant.now(clock));
        for (int index = 0; index < request.candidateTrades().size(); index++) {
            TradeRequest trade = request.candidateTrades().get(index);
            run.addTrade(new OptimizationTradeEntity(index, trade.tradeName(), trade.marginRequired(),
                    trade.expectedPnl(), selectedIndices.contains(index)));
        }
        return toResponse(repository.save(run));
    }

    @Transactional(readOnly = true)
    public OptimizationResponse get(UUID requestId) {
        return repository.findById(requestId)
                .map(this::toResponse)
                .orElseThrow(() -> new OptimizationNotFoundException(requestId));
    }

    @Transactional(readOnly = true)
    public PagedOptimizationResponse getAll(int page, int size) {
        Page<OptimizationRunEntity> result = repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        List<OptimizationResponse> content = result.getContent().stream().map(this::toResponse).toList();
        return new PagedOptimizationResponse(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private OptimizationResponse toResponse(OptimizationRunEntity run) {
        List<TradeResponse> selectedTrades = run.getTrades().stream()
                .filter(OptimizationTradeEntity::isSelected)
                .map(trade -> new TradeResponse(trade.getTradeName(), trade.getMarginRequired(), trade.getExpectedPnl()))
                .toList();
        return new OptimizationResponse(run.getRequestId(), selectedTrades, run.getTotalMarginRequired(),
                run.getTotalExpectedPnl(), run.getCreatedAt());
    }
}
