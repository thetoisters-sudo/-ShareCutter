package com.sharecutter.backend.repository;

import com.sharecutter.backend.domain.entity.PortfolioSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioSnapshotRepository
        extends JpaRepository<
                PortfolioSnapshotEntity,
                UUID
        > {

    List<PortfolioSnapshotEntity>
    findAllByPortfolioIdOrderByCapturedAtAsc(
            UUID portfolioId
    );

    Optional<PortfolioSnapshotEntity>
    findTopByPortfolioIdOrderByCapturedAtDesc(
            UUID portfolioId
    );
}