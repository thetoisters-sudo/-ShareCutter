package com.sharecutter.backend.repository;

import com.sharecutter.backend.domain.entity.WeeklyPortfolioTargetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeeklyPortfolioTargetRepository
        extends JpaRepository<WeeklyPortfolioTargetEntity, UUID> {

    Optional<WeeklyPortfolioTargetEntity>
    findByIdAndPortfolioUserIdAndDeletedAtIsNull(
            UUID id,
            UUID userId
    );

    Optional<WeeklyPortfolioTargetEntity>
    findByPortfolioIdAndAssetIdAndWeekStartDateAndDeletedAtIsNull(
            UUID portfolioId,
            UUID assetId,
            LocalDate weekStartDate
    );

    List<WeeklyPortfolioTargetEntity>
    findAllByPortfolioIdAndWeekStartDateAndDeletedAtIsNullOrderByAssetSymbolAsc(
            UUID portfolioId,
            LocalDate weekStartDate
    );

    List<WeeklyPortfolioTargetEntity>
    findAllByPortfolioIdAndDeletedAtIsNullOrderByWeekStartDateDescAssetSymbolAsc(
            UUID portfolioId
    );

    boolean
    existsByPortfolioIdAndAssetIdAndWeekStartDateAndDeletedAtIsNull(
            UUID portfolioId,
            UUID assetId,
            LocalDate weekStartDate
    );

    void deleteAllByPortfolioIdAndWeekStartDateAndDeletedAtIsNull(
            UUID portfolioId,
            LocalDate weekStartDate
    );
}