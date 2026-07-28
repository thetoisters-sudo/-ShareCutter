package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.enums.TransactionType;
import com.sharecutter.backend.dto.analytics.PortfolioSummaryResponse;
import com.sharecutter.backend.repository.AssetRepository;
import com.sharecutter.backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PortfolioAnalyticsService {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

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