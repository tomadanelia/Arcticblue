package com.arcticblue.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OptimizationRunRepository extends JpaRepository<OptimizationRunEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = "trades")
    Optional<OptimizationRunEntity> findById(UUID requestId);

    Page<OptimizationRunEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
