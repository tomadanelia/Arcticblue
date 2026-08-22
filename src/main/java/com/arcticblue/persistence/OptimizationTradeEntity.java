package com.arcticblue.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "optimization_trades")
public class OptimizationTradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private OptimizationRunEntity optimizationRun;

    @Column(name = "candidate_order", nullable = false)
    private int candidateOrder;

    @Column(name = "trade_name", nullable = false, length = 200)
    private String tradeName;

    @Column(name = "margin_required", nullable = false)
    private long marginRequired;

    @Column(name = "expected_pnl", nullable = false)
    private long expectedPnl;

    @Column(name = "selected", nullable = false)
    private boolean selected;

    protected OptimizationTradeEntity() {
    }

    public OptimizationTradeEntity(int candidateOrder, String tradeName, long marginRequired,
                                   long expectedPnl, boolean selected) {
        this.candidateOrder = candidateOrder;
        this.tradeName = tradeName;
        this.marginRequired = marginRequired;
        this.expectedPnl = expectedPnl;
        this.selected = selected;
    }

    void setOptimizationRun(OptimizationRunEntity optimizationRun) {
        this.optimizationRun = optimizationRun;
    }

    public String getTradeName() {
        return tradeName;
    }

    public long getMarginRequired() {
        return marginRequired;
    }

    public long getExpectedPnl() {
        return expectedPnl;
    }

    public boolean isSelected() {
        return selected;
    }
}
