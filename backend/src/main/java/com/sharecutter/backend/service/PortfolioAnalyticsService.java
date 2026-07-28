package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.enums.TransactionType;
import com.sharecutter.backend.dto.analytics.AssetAllocationItemResponse;
import com.sharecutter.backend.dto.analytics.PortfolioAllocationResponse;
import com.sharecutter.backend.dto.analytics.PortfolioSummaryResponse;
import com.sharecutter.backend.repository.AssetRepository;
import com.sharecutter.backend.repository.TransactionRepository;
import com.sharecutter.backend.repository.projection.AssetAllocationProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PortfolioAnalyticsService {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    private static final int PERCENT_SCALE = 6;

    private final PortfolioService portfolioService;
    private final AssetRepository assetRepository;
    private final TransactionRepository transactionRepository;

    public PortfolioAnalyticsService(
            PortfolioService portfolioService,
            AssetRepository assetRepository,
            TransactionRepository transactionRepository
    ) {
        this.portfolioService = portfolioService;
        this.assetRepository = assetRepository;
        this.transactionRepository = transactionRepository;
    }

    public PortfolioSummaryResponse getPortfolioSummary(
            UUID userId,
            UUID portfolioId
    ) {
        PortfolioEntity portfolio =
                portfolioService.getPortfolio(
                        userId,
                        portfolioId
                );

        long activeAssetCount =
                assetRepository
                        .countByPortfolioIdAndDeletedAtIsNull(
                                portfolioId
                        );

        long transactionCount =
                transactionRepository
                        .countByPortfolioIdAndDeletedAtIsNull(
                                portfolioId
                        );

        BigDecimal totalBuyAmount =
                getTransactionTotal(
                        portfolioId,
                        TransactionType.BUY
                );

        BigDecimal totalSellAmount =
                getTransactionTotal(
                        portfolioId,
                        TransactionType.SELL
                );

        BigDecimal totalDividendAmount =
                getTransactionTotal(
                        portfolioId,
                        TransactionType.DIVIDEND
                );

        BigDecimal totalFeeAmount =
                getTransactionTotal(
                        portfolioId,
                        TransactionType.FEE
                );

        BigDecimal totalDepositAmount =
                getTransactionTotal(
                        portfolioId,
                        TransactionType.DEPOSIT
                );

        BigDecimal totalWithdrawalAmount =
                getTransactionTotal(
                        portfolioId,
                        TransactionType.WITHDRAWAL
                );

        BigDecimal totalRealizedProfit =
                zeroIfNull(
                        portfolio.getTotalRealizedProfit()
                );

        BigDecimal totalUnrealizedProfit =
                zeroIfNull(
                        portfolio.getTotalUnrealizedProfit()
                );

        BigDecimal totalProfit =
                totalRealizedProfit.add(
                        totalUnrealizedProfit
                );

        BigDecimal netCashFlow =
                totalDepositAmount
                        .add(totalSellAmount)
                        .add(totalDividendAmount)
                        .subtract(totalBuyAmount)
                        .subtract(totalWithdrawalAmount)
                        .subtract(totalFeeAmount);

        return new PortfolioSummaryResponse(
                portfolio.getId(),
                portfolio.getName(),
                portfolio.getInitialValue(),
                portfolio.getCurrentValue(),
                totalRealizedProfit,
                totalUnrealizedProfit,
                totalProfit,
                portfolio.getTotalReturnPercent(),
                activeAssetCount,
                transactionCount,
                totalBuyAmount,
                totalSellAmount,
                totalDividendAmount,
                totalFeeAmount,
                totalDepositAmount,
                totalWithdrawalAmount,
                netCashFlow,
                OffsetDateTime.now(
                        ZoneOffset.UTC
                )
        );
    }

    public PortfolioAllocationResponse getPortfolioAllocation(
            UUID userId,
            UUID portfolioId
    ) {
        PortfolioEntity portfolio =
                portfolioService.getPortfolio(
                        userId,
                        portfolioId
                );

        List<AssetAllocationProjection>
                allocationProjections =
                transactionRepository
                        .findAssetAllocationByPortfolioId(
                                portfolioId
                        );

        BigDecimal totalNetInvestedAmount =
                calculateTotalNetInvestedAmount(
                        allocationProjections
                );

        List<AssetAllocationItemResponse>
                allocationItems =
                createAllocationItems(
                        allocationProjections,
                        totalNetInvestedAmount
                );

        return new PortfolioAllocationResponse(
                portfolio.getId(),
                portfolio.getName(),
                totalNetInvestedAmount,
                allocationItems.size(),
                allocationItems,
                OffsetDateTime.now(
                        ZoneOffset.UTC
                )
        );
    }

    private BigDecimal calculateTotalNetInvestedAmount(
            List<AssetAllocationProjection>
                    allocationProjections
    ) {
        return allocationProjections
                .stream()
                .map(
                        this::
                                calculateNonNegativeNetInvestedAmount
                )
                .reduce(
                        ZERO,
                        BigDecimal::add
                );
    }

    private List<AssetAllocationItemResponse>
    createAllocationItems(
            List<AssetAllocationProjection>
                    allocationProjections,
            BigDecimal totalNetInvestedAmount
    ) {
        return allocationProjections
                .stream()
                .map(
                        projection ->
                                createAllocationItem(
                                        projection,
                                        totalNetInvestedAmount
                                )
                )
                .sorted(
                        Comparator
                                .comparing(
                                        AssetAllocationItemResponse::
                                                netInvestedAmount
                                )
                                .reversed()
                                .thenComparing(
                                        AssetAllocationItemResponse::
                                                symbol
                                )
                )
                .toList();
    }

    private AssetAllocationItemResponse
    createAllocationItem(
            AssetAllocationProjection projection,
            BigDecimal totalNetInvestedAmount
    ) {
        BigDecimal boughtQuantity =
                zeroIfNull(
                        projection.getBoughtQuantity()
                );

        BigDecimal soldQuantity =
                zeroIfNull(
                        projection.getSoldQuantity()
                );

        BigDecimal totalBuyAmount =
                zeroIfNull(
                        projection.getTotalBuyAmount()
                );

        BigDecimal totalSellAmount =
                zeroIfNull(
                        projection.getTotalSellAmount()
                );

        BigDecimal currentQuantity =
                boughtQuantity.subtract(
                        soldQuantity
                );

        BigDecimal netInvestedAmount =
                calculateNonNegativeNetInvestedAmount(
                        totalBuyAmount,
                        totalSellAmount
                );

        BigDecimal allocationPercent =
                calculateAllocationPercent(
                        netInvestedAmount,
                        totalNetInvestedAmount
                );

        return new AssetAllocationItemResponse(
                projection.getAssetId(),
                projection.getSymbol(),
                projection.getDisplayName(),
                projection.getAssetType(),
                projection.getCurrency(),
                boughtQuantity,
                soldQuantity,
                currentQuantity,
                totalBuyAmount,
                totalSellAmount,
                netInvestedAmount,
                allocationPercent
        );
    }

    private BigDecimal
    calculateNonNegativeNetInvestedAmount(
            AssetAllocationProjection projection
    ) {
        return calculateNonNegativeNetInvestedAmount(
                zeroIfNull(
                        projection.getTotalBuyAmount()
                ),
                zeroIfNull(
                        projection.getTotalSellAmount()
                )
        );
    }

    private BigDecimal
    calculateNonNegativeNetInvestedAmount(
            BigDecimal totalBuyAmount,
            BigDecimal totalSellAmount
    ) {
        BigDecimal netInvestedAmount =
                totalBuyAmount.subtract(
                        totalSellAmount
                );

        return netInvestedAmount.signum() < 0
                ? ZERO
                : netInvestedAmount;
    }

    private BigDecimal calculateAllocationPercent(
            BigDecimal netInvestedAmount,
            BigDecimal totalNetInvestedAmount
    ) {
        if (totalNetInvestedAmount.signum()
                <= 0) {
            return ZERO;
        }

        return netInvestedAmount
                .multiply(ONE_HUNDRED)
                .divide(
                        totalNetInvestedAmount,
                        PERCENT_SCALE,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal getTransactionTotal(
            UUID portfolioId,
            TransactionType transactionType
    ) {
        BigDecimal total =
                transactionRepository
                        .sumTotalAmountByPortfolioIdAndTransactionType(
                                portfolioId,
                                transactionType
                        );

        return zeroIfNull(total);
    }

    private BigDecimal zeroIfNull(
            BigDecimal value
    ) {
        return value == null
                ? ZERO
                : value;
    }
}