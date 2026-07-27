package com.sharecutter.backend.repository;

import com.sharecutter.backend.domain.entity.AssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository
        extends JpaRepository<AssetEntity, UUID> {

    Optional<AssetEntity> findByIdAndDeletedAtIsNull(
            UUID id
    );

    Optional<AssetEntity>
    findByIdAndPortfolioIdAndDeletedAtIsNull(
            UUID id,
            UUID portfolioId
    );

    List<AssetEntity>
    findAllByPortfolioIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            UUID portfolioId
    );

    Optional<AssetEntity>
    findByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
            UUID portfolioId,
            String symbol
    );

    boolean
    existsByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
            UUID portfolioId,
            String symbol
    );
}