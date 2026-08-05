package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.PortfolioHoldingEntity;
import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.domain.enums.TransactionType;
import com.sharecutter.backend.dto.analytics.AssetAllocationItemResponse;
import com.sharecutter.backend.dto.analytics.PortfolioAllocationResponse;
import com.sharecutter.backend.dto.analytics.PortfolioSummaryResponse;
import com.sharecutter.backend.exception.InvalidTransactionException;
import com.sharecutter.backend.repository.PortfolioHoldingRepository;
import com.sharecutter.backend.repository.TransactionRepository;
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

    private static final int QUANTITY_SCALE = 8;

    private static final int MONEY_SCALE = 8;

    private final PortfolioService portfolioService;

    private final TransactionRepository
            transactionRepository;

    private final PortfolioHoldingRepository
            portfolioHoldingRepository;

    public PortfolioAnalyticsService(
            PortfolioService portfolioService,
            TransactionRepository transactionRepository,
            PortfolioHoldingRepository
                    portfolioHoldingRepository
    ) {
        this.portfolioService =
                portfolioService;

        this.transactionRepository =
                transactionRepository;

        this.portfolioHoldingRepository =
                portfolioHoldingRepository;
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
                portfolioHoldingRepository
                        .countByPortfolioIdAndQuantityGreaterThanAndDeletedAtIsNull(
                                portfolioId,
                                ZERO
                        );

        List<TransactionEntity> transactions =
                transactionRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                                portfolioId
                        );

        long transactionCount =
                transactions.size();

        BigDecimal totalBuyAmount =
                getTransactionTotal(
                        transactions,
                        TransactionType.BUY
                );

        BigDecimal totalSellAmount =
                getTransactionTotal(
                        transactions,
                        TransactionType.SELL
                );

        BigDecimal totalDividendAmount =
                getTransactionTotal(
                        transactions,
                        TransactionType.DIVIDEND
                );

        BigDecimal totalFeeTransactionsAmount =
                getTransactionTotal(
                        transactions,
                        TransactionType.FEE
                );

        BigDecimal totalExplicitFees =
                transactions
                        .stream()
                        .map(
                                TransactionEntity::getFee
                        )
                        .map(
                                this::zeroIfNull
                        )
                        .reduce(
                                ZERO,
                                BigDecimal::add
                        );

        BigDecimal totalFeeAmount =
                totalFeeTransactionsAmount.add(
                        totalExplicitFees
                );

        BigDecimal totalDepositAmount =
                getTransactionTotal(
                        transactions,
                        TransactionType.DEPOSIT
                );

        BigDecimal totalWithdrawalAmount =
                getTransactionTotal(
                        transactions,
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
                        .add(
                                totalSellAmount
                        )
                        .add(
                                totalDividendAmount
                        )
                        .subtract(
                                totalBuyAmount
                        )
                        .subtract(
                                totalWithdrawalAmount
                        )
                        .subtract(
                                totalFeeAmount
                        );

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

    public PortfolioAllocationResponse
    getPortfolioAllocation(
            UUID userId,
            UUID portfolioId
    ) {
        PortfolioEntity portfolio =
                portfolioService.getPortfolio(
                        userId,
                        portfolioId
                );

        List<PortfolioHoldingEntity> holdings =
                portfolioHoldingRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                                portfolioId
                        )
                        .stream()
                        .filter(
                                holding ->
                                        !holding.isEmpty()
                        )
                        .toList();

        BigDecimal portfolioValue =
                zeroIfNull(
                        portfolio.getCurrentValue()
                );

        BigDecimal totalMarketValue =
                holdings
                        .stream()
                        .map(
                                PortfolioHoldingEntity::
                                        getMarketValue
                        )
                        .map(
                                this::zeroIfNull
                        )
                        .reduce(
                                ZERO,
                                BigDecimal::add
                        );

        BigDecimal cashBalance =
                portfolioValue.subtract(
                        totalMarketValue
                );

        BigDecimal totalCost =
                holdings
                        .stream()
                        .map(
                                PortfolioHoldingEntity::
                                        getTotalCost
                        )
                        .map(
                                this::zeroIfNull
                        )
                        .reduce(
                                ZERO,
                                BigDecimal::add
                        );

        BigDecimal totalRealizedProfit =
                holdings
                        .stream()
                        .map(
                                PortfolioHoldingEntity::
                                        getRealizedProfit
                        )
                        .map(
                                this::zeroIfNull
                        )
                        .reduce(
                                ZERO,
                                BigDecimal::add
                        );

        BigDecimal totalUnrealizedProfit =
                holdings
                        .stream()
                        .map(
                                PortfolioHoldingEntity::
                                        getUnrealizedProfit
                        )
                        .map(
                                this::zeroIfNull
                        )
                        .reduce(
                                ZERO,
                                BigDecimal::add
                        );

        BigDecimal totalTargetWeightPercent =
                holdings
                        .stream()
                        .map(
                                PortfolioHoldingEntity::
                                        getTargetWeightPercent
                        )
                        .map(
                                this::zeroIfNull
                        )
                        .reduce(
                                ZERO,
                                BigDecimal::add
                        );

        List<AssetAllocationItemResponse>
                allocationItems =
                holdings
                        .stream()
                        .map(
                                holding ->
                                        createAllocationItem(
                                                holding,
                                                portfolioValue
                                        )
                        )
                        .sorted(
                                Comparator
                                        .comparing(
                                                AssetAllocationItemResponse::
                                                        marketValue
                                        )
                                        .reversed()
                                        .thenComparing(
                                                AssetAllocationItemResponse::
                                                        symbol
                                        )
                        )
                        .toList();

        return new PortfolioAllocationResponse(
                portfolio.getId(),
                portfolio.getName(),
                portfolioValue,
                cashBalance,
                totalMarketValue,
                totalCost,
                totalRealizedProfit,
                totalUnrealizedProfit,
                totalTargetWeightPercent,
                allocationItems.size(),
                allocationItems,
                OffsetDateTime.now(
                        ZoneOffset.UTC
                )
        );
    }

    @Transactional
    public PortfolioAllocationResponse
    updateTargetWeight(
            UUID userId,
            UUID portfolioId,
            UUID assetId,
            BigDecimal targetWeightPercent
    ) {
        portfolioService.getPortfolio(
                userId,
                portfolioId
        );

        PortfolioHoldingEntity holding =
                portfolioHoldingRepository
                        .findByPortfolioIdAndAssetIdAndDeletedAtIsNull(
                                portfolioId,
                                assetId
                        )
                        .orElseThrow(
                                () ->
                                        new InvalidTransactionException(
                                                "Portfolio holding was not found "
                                                        + "for the selected asset"
                                        )
                        );

        holding.updateTargetWeightPercent(
                targetWeightPercent
        );

        portfolioHoldingRepository.save(
                holding
        );

        return getPortfolioAllocation(
                userId,
                portfolioId
        );
    }

    private AssetAllocationItemResponse
    createAllocationItem(
            PortfolioHoldingEntity holding,
            BigDecimal portfolioValue
    ) {
        AssetEntity asset =
                holding.getAsset();

        BigDecimal quantity =
                zeroIfNull(
                        holding.getQuantity()
                );

        BigDecimal currentPrice =
                zeroIfNull(
                        holding.getCurrentPrice()
                );

        BigDecimal marketValue =
                zeroIfNull(
                        holding.getMarketValue()
                );

        BigDecimal targetWeightPercent =
                zeroIfNull(
                        holding.getTargetWeightPercent()
                );

        BigDecimal allocationPercent =
                calculateAllocationPercent(
                        marketValue,
                        portfolioValue
                );

        BigDecimal targetMarketValue =
                calculateTargetMarketValue(
                        portfolioValue,
                        targetWeightPercent
                );

        BigDecimal targetQuantity =
                calculateTargetQuantity(
                        targetMarketValue,
                        currentPrice
                );

        BigDecimal quantityDifference =
                targetQuantity.subtract(
                        quantity
                );

        BigDecimal estimatedTradeValue =
                quantityDifference
                        .abs()
                        .multiply(
                                currentPrice
                        )
                        .setScale(
                                QUANTITY_SCALE,
                                RoundingMode.HALF_UP
                        );

        String rebalanceAction =
                determineRebalanceAction(
                        quantityDifference
                );

        return new AssetAllocationItemResponse(
                asset.getId(),
                asset.getSymbol(),
                asset.getDisplayName(),
                asset.getAssetType(),
                asset.getCurrency(),
                quantity,
                holding.getAverageCost(),
                currentPrice,
                holding.getTotalCost(),
                marketValue,
                holding.getRealizedProfit(),
                holding.getUnrealizedProfit(),
                allocationPercent,
                targetWeightPercent,
                targetMarketValue,
                targetQuantity,
                quantityDifference,
                estimatedTradeValue,
                rebalanceAction
        );
    }

    private BigDecimal calculateAllocationPercent(
            BigDecimal marketValue,
            BigDecimal portfolioValue
    ) {
        if (portfolioValue.signum() <= 0) {
            return ZERO;
        }

        return marketValue
                .multiply(
                        ONE_HUNDRED
                )
                .divide(
                        portfolioValue,
                        PERCENT_SCALE,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal calculateTargetMarketValue(
            BigDecimal portfolioValue,
            BigDecimal targetWeightPercent
    ) {
        if (
                portfolioValue.signum() <= 0
                        || targetWeightPercent.signum() <= 0
        ) {
            return ZERO;
        }

        return portfolioValue
                .multiply(
                        targetWeightPercent
                )
                .divide(
                        ONE_HUNDRED,
                        QUANTITY_SCALE,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal calculateTargetQuantity(
            BigDecimal targetMarketValue,
            BigDecimal currentPrice
    ) {
        if (
                targetMarketValue.signum() <= 0
                        || currentPrice.signum() <= 0
        ) {
            return ZERO;
        }

        return targetMarketValue.divide(
                currentPrice,
                QUANTITY_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private String determineRebalanceAction(
            BigDecimal quantityDifference
    ) {
        int comparison =
                quantityDifference.compareTo(
                        ZERO
                );

        if (comparison > 0) {
            return "BUY";
        }

        if (comparison < 0) {
            return "SELL";
        }

        return "HOLD";
    }

    private BigDecimal getTransactionTotal(
            List<TransactionEntity> transactions,
            TransactionType transactionType
    ) {
        return transactions
                .stream()
                .filter(
                        transaction ->
                                transaction.getTransactionType()
                                        == transactionType
                )
                .map(
                        this::resolveEffectiveTransactionAmount
                )
                .reduce(
                        ZERO,
                        BigDecimal::add
                );
    }

    private BigDecimal resolveEffectiveTransactionAmount(
            TransactionEntity transaction
    ) {
        TransactionType transactionType =
                transaction.getTransactionType();

        if (
                transactionType == TransactionType.BUY
                        || transactionType
                        == TransactionType.SELL
        ) {
            BigDecimal quantity =
                    transaction.getQuantity();

            BigDecimal unitPrice =
                    transaction.getUnitPrice();

            if (
                    quantity != null
                            && unitPrice != null
            ) {
                return quantity
                        .multiply(
                                unitPrice
                        )
                        .setScale(
                                MONEY_SCALE,
                                RoundingMode.HALF_UP
                        );
            }
        }

        return zeroIfNull(
                transaction.getTotalAmount()
        );
    }

    private BigDecimal zeroIfNull(
            BigDecimal value
    ) {
        return value == null
                ? ZERO
                : value;
    }
}