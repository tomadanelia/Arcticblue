package com.arcticblue.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "optimization_runs")
public class OptimizationRunEntity {

    @Id
    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    @Column(name = "max_margin", nullable = false)
    private long maxMargin;

    @Column(name = "total_margin_required", nullable = false)
    private long totalMarginRequired;

    @Column(name = "total_expected_pnl", nullable = false)
    private long totalExpectedPnl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "optimizationRun", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("candidateOrder ASC")
    private List<OptimizationTradeEntity> trades = new ArrayList<>();

    protected OptimizationRunEntity() {
    }

    public OptimizationRunEntity(UUID requestId, long maxMargin, long totalMarginRequired,
                                 long totalExpectedPnl, Instant createdAt) {
        this.requestId = requestId;
        this.maxMargin = maxMargin;
        this.totalMarginRequired = totalMarginRequired;
        this.totalExpectedPnl = totalExpectedPnl;
        this.createdAt = createdAt;
    }

    public void addTrade(OptimizationTradeEntity trade) {
        trades.add(trade);
        trade.setOptimizationRun(this);
    }

    public UUID getRequestId() {
        return requestId;
    }

    public long getMaxMargin() {
        return maxMargin;
    }

    public long getTotalMarginRequired() {
        return totalMarginRequired;
    }

    public long getTotalExpectedPnl() {
        return totalExpectedPnl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OptimizationTradeEntity> getTrades() {
        return trades;
    }
}
