CREATE TABLE optimization_runs (
    request_id UUID PRIMARY KEY,
    max_margin BIGINT NOT NULL CHECK (max_margin >= 0),
    total_margin_required BIGINT NOT NULL CHECK (total_margin_required >= 0),
    total_expected_pnl BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE optimization_trades (
    id BIGSERIAL PRIMARY KEY,
    request_id UUID NOT NULL REFERENCES optimization_runs(request_id) ON DELETE CASCADE,
    candidate_order INTEGER NOT NULL,
    trade_name VARCHAR(200) NOT NULL,
    margin_required BIGINT NOT NULL CHECK (margin_required > 0),
    expected_pnl BIGINT NOT NULL,
    selected BOOLEAN NOT NULL,
    CONSTRAINT uk_optimization_trade_order UNIQUE (request_id, candidate_order)
);

CREATE INDEX idx_optimization_runs_created_at
    ON optimization_runs (created_at DESC);

CREATE INDEX idx_optimization_trades_request_selected
    ON optimization_trades (request_id, selected);
