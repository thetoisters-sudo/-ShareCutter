package com.sharecutter.backend.repository;

import com.sharecutter.backend.domain.entity.PortfolioHoldingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioHoldingRepository
        extends JpaRepository<PortfolioHoldingEntity, UUID> {

    Optional<PortfolioHoldingEntity>
    findByIdAndDeletedAtIsNull(
            UUID id
    );

    Optional<PortfolioHoldingEntity>
    findByPortfolioIdAndAssetIdAndDeletedAtIsNull(
            UUID portfolioId,
            UUID assetId
    );

    List<PortfolioHoldingEntity>
    findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
            UUID portfolioId
    );

    List<PortfolioHoldingEntity>
    findAllByAssetIdAndDeletedAtIsNull(
            UUID assetId
    );

    boolean
    existsByPortfolioIdAndAssetIdAndDeletedAtIsNull(
            UUID portfolioId,
            UUID assetId
    );

    long countByPortfolioIdAndDeletedAtIsNull(
            UUID portfolioId
    );

    long
    countByPortfolioIdAndQuantityGreaterThanAndDeletedAtIsNull(
            UUID portfolioId,
            BigDecimal quantity
    );

    @Query("""
            select coalesce(
                sum(holding.marketValue),
                0
            )
            from PortfolioHoldingEntity holding
            where holding.portfolio.id = :portfolioId
              and holding.deletedAt is null
            """)
    BigDecimal sumMarketValueByPortfolioId(
            @Param("portfolioId")
            UUID portfolioId
    );

    @Query("""
            select coalesce(
                sum(holding.totalCost),
                0
            )
            from PortfolioHoldingEntity holding
            where holding.portfolio.id = :portfolioId
              and holding.deletedAt is null
            """)
    BigDecimal sumTotalCostByPortfolioId(
            @Param("portfolioId")
            UUID portfolioId
    );

    @Query("""
            select coalesce(
                sum(holding.realizedProfit),
                0
            )
            from PortfolioHoldingEntity holding
            where holding.portfolio.id = :portfolioId
              and holding.deletedAt is null
            """)
    BigDecimal sumRealizedProfitByPortfolioId(
            @Param("portfolioId")
            UUID portfolioId
    );

    @Query("""
            select coalesce(
                sum(holding.unrealizedProfit),
                0
            )
            from PortfolioHoldingEntity holding
            where holding.portfolio.id = :portfolioId
              and holding.deletedAt is null
            """)
    BigDecimal sumUnrealizedProfitByPortfolioId(
            @Param("portfolioId")
            UUID portfolioId
    );
}