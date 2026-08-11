package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.PortfolioHoldingEntity;
import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.domain.enums.TransactionType;
import com.sharecutter.backend.dto.allocationpurchase.AllocationPurchaseExecuteRequest;
import com.sharecutter.backend.dto.allocationpurchase.AllocationPurchaseExecutionResponse;
import com.sharecutter.backend.dto.allocationpurchase.AllocationPurchasePreviewRequest;
import com.sharecutter.backend.dto.allocationpurchase.AllocationPurchasePreviewResponse;
import com.sharecutter.backend.dto.marketdata.MarketPriceResponse;
import com.sharecutter.backend.exception.InvalidTransactionException;
import com.sharecutter.backend.repository.PortfolioHoldingRepository;
import com.sharecutter.backend.service.marketdata.MarketDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PortfolioAllocationPurchaseService {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    private static final int QUANTITY_SCALE = 8;

    private static final int MONEY_SCALE = 8;

    private static final int WEIGHT_SCALE = 6;

    private final PortfolioService
            portfolioService;

    private final AssetService
            assetService;

    private final PortfolioHoldingRepository
            portfolioHoldingRepository;

    private final MarketDataService
            marketDataService;

    private final TransactionService
            transactionService;

    public PortfolioAllocationPurchaseService(
            PortfolioService portfolioService,
            AssetService assetService,
            PortfolioHoldingRepository portfolioHoldingRepository,
            MarketDataService marketDataService,
            TransactionService transactionService
    ) {
        this.portfolioService =
                portfolioService;

        this.assetService =
                assetService;

        this.portfolioHoldingRepository =
                portfolioHoldingRepository;

        this.marketDataService =
                marketDataService;

        this.transactionService =
                transactionService;
    }

    public AllocationPurchasePreviewResponse
    previewPurchase(
            UUID userId,
            UUID portfolioId,
            AllocationPurchasePreviewRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Allocation preview request must not be null"
            );
        }

        return calculatePreview(
                userId,
                portfolioId,
                request.assetId(),
                request.targetWeightPercent()
        );
    }

    @Transactional
    public AllocationPurchaseExecutionResponse
    executePurchase(
            UUID userId,
            UUID portfolioId,
            AllocationPurchaseExecuteRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Allocation execution request must not be null"
            );
        }

        AllocationPurchasePreviewResponse preview =
                calculatePreview(
                        userId,
                        portfolioId,
                        request.assetId(),
                        request.targetWeightPercent()
                );

        validateExecutablePreview(
                preview
        );

        PortfolioEntity portfolio =
                portfolioService.getPortfolio(
                        userId,
                        portfolioId
                );

        AssetEntity asset =
                assetService.getAsset(
                        userId,
                        portfolioId,
                        request.assetId()
                );

        BigDecimal normalizedFee =
                normalizeFee(
                        request.fee()
                );

        BigDecimal availableCashBefore =
                calculateAvailableCash(
                        portfolio
                );

        TransactionType transactionType =
                resolveTransactionType(
                        preview.suggestedAction()
                );

        BigDecimal tradedQuantity =
                resolveTradedQuantity(
                        preview
                );

        BigDecimal tradeAmount =
                preview
                        .estimatedTradeAmount()
                        .setScale(
                                MONEY_SCALE,
                                RoundingMode.HALF_UP
                        );

        BigDecimal cashImpact =
                calculateCashImpact(
                        transactionType,
                        tradeAmount,
                        normalizedFee
                );

        BigDecimal availableCashAfter =
                availableCashBefore
                        .add(
                                cashImpact
                        )
                        .setScale(
                                MONEY_SCALE,
                                RoundingMode.HALF_UP
                        );

        if (
                availableCashAfter.compareTo(
                        ZERO
                ) < 0
        ) {
            throw new InvalidTransactionException(
                    "Insufficient portfolio cash. "
                            + "Available cash: "
                            + availableCashBefore
                            + ", trade cash impact: "
                            + cashImpact
            );
        }

        OffsetDateTime executionTime =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                );

        TransactionEntity transaction =
                transactionService.createTransaction(
                        userId,
                        portfolioId,
                        asset.getId(),
                        transactionType,
                        tradedQuantity,
                        preview.currentPrice(),
                        normalizedFee,
                        tradeAmount,
                        asset.getCurrency(),
                        executionTime,
                        createTransactionNotes(
                                preview
                        )
                );

        PortfolioHoldingEntity holding =
                portfolioHoldingRepository
                        .findByPortfolioIdAndAssetIdAndDeletedAtIsNull(
                                portfolioId,
                                asset.getId()
                        )
                        .orElse(null);

        if (
                holding != null
        ) {
            holding.updateTargetWeightPercent(
                    preview.targetWeightPercent()
            );

            portfolioHoldingRepository.save(
                    holding
            );
        }

        return new AllocationPurchaseExecutionResponse(
                portfolio.getId(),
                portfolio.getName(),
                asset.getId(),
                asset.getSymbol(),
                asset.getDisplayName(),
                transaction.getId(),
                preview.suggestedAction(),
                preview.targetWeightPercent(),
                preview.currentPrice(),
                tradedQuantity,
                tradeAmount,
                normalizedFee,
                cashImpact,
                availableCashBefore,
                availableCashAfter,
                preview.totalTargetWeightPercent(),
                preview.allocationDifferencePercent(),
                asset.getCurrency(),
                executionTime
        );
    }

    private AllocationPurchasePreviewResponse
    calculatePreview(
            UUID userId,
            UUID portfolioId,
            UUID assetId,
            BigDecimal targetWeightPercent
    ) {
        PortfolioEntity portfolio =
                portfolioService.getPortfolio(
                        userId,
                        portfolioId
                );

        AssetEntity asset =
                assetService.getAsset(
                        userId,
                        portfolioId,
                        assetId
                );

        BigDecimal normalizedTargetWeight =
                normalizeTargetWeight(
                        targetWeightPercent
                );

        List<PortfolioHoldingEntity> holdings =
                portfolioHoldingRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                                portfolioId
                        );

        BigDecimal weightAssignedToOtherAssets =
                calculateWeightAssignedToOtherAssets(
                        holdings,
                        asset.getId()
                );

        PortfolioHoldingEntity existingHolding =
                findExistingHolding(
                        holdings,
                        asset.getId()
                );

        BigDecimal existingQuantity =
                existingHolding == null
                        ? ZERO
                        : zeroIfNull(
                                existingHolding
                                        .getQuantity()
                        );

        MarketPriceResponse marketPrice =
                marketDataService
                        .getLatestPrice(
                                asset.getSymbol(),
                                asset.getExchange()
                        );

        BigDecimal currentPrice =
                requirePositivePrice(
                        marketPrice.price()
                );

        BigDecimal portfolioCurrentValue =
                requirePositivePortfolioValue(
                        portfolio.getCurrentValue()
                );

        BigDecimal currentMarketValue =
                existingQuantity
                        .multiply(
                                currentPrice
                        )
                        .setScale(
                                MONEY_SCALE,
                                RoundingMode.HALF_UP
                        );

        BigDecimal targetMarketValue =
                portfolioCurrentValue
                        .multiply(
                                normalizedTargetWeight
                        )
                        .divide(
                                ONE_HUNDRED,
                                MONEY_SCALE,
                                RoundingMode.DOWN
                        );

        BigDecimal targetQuantity =
                targetMarketValue.divide(
                        currentPrice,
                        QUANTITY_SCALE,
                        RoundingMode.DOWN
                );

        BigDecimal quantityDifference =
                targetQuantity
                        .subtract(
                                existingQuantity
                        )
                        .setScale(
                                QUANTITY_SCALE,
                                RoundingMode.DOWN
                        );

        BigDecimal quantityToBuy =
                quantityDifference.signum() > 0
                        ? quantityDifference
                        : ZERO.setScale(
                                QUANTITY_SCALE,
                                RoundingMode.DOWN
                        );

        BigDecimal quantityToSell =
                quantityDifference.signum() < 0
                        ? quantityDifference
                                .abs()
                                .setScale(
                                        QUANTITY_SCALE,
                                        RoundingMode.DOWN
                                )
                        : ZERO.setScale(
                                QUANTITY_SCALE,
                                RoundingMode.DOWN
                        );

        BigDecimal estimatedTradeAmount =
                quantityDifference
                        .abs()
                        .multiply(
                                currentPrice
                        )
                        .setScale(
                                MONEY_SCALE,
                                RoundingMode.HALF_UP
                        );

        BigDecimal totalTargetWeight =
                weightAssignedToOtherAssets
                        .add(
                                normalizedTargetWeight
                        )
                        .setScale(
                                WEIGHT_SCALE,
                                RoundingMode.HALF_UP
                        );

        BigDecimal allocationDifference =
                ONE_HUNDRED
                        .subtract(
                                totalTargetWeight
                        )
                        .setScale(
                                WEIGHT_SCALE,
                                RoundingMode.HALF_UP
                        );

        String suggestedAction =
                resolveSuggestedAction(
                        quantityDifference
                );

        return new AllocationPurchasePreviewResponse(
                portfolio.getId(),
                portfolio.getName(),
                asset.getId(),
                asset.getSymbol(),
                asset.getDisplayName(),
                asset.getExchange(),
                asset.getCurrency(),
                portfolioCurrentValue.setScale(
                        MONEY_SCALE,
                        RoundingMode.HALF_UP
                ),
                currentPrice.setScale(
                        MONEY_SCALE,
                        RoundingMode.HALF_UP
                ),
                normalizedTargetWeight,
                weightAssignedToOtherAssets,
                totalTargetWeight,
                allocationDifference,
                currentMarketValue,
                targetMarketValue,
                existingQuantity.setScale(
                        QUANTITY_SCALE,
                        RoundingMode.HALF_UP
                ),
                targetQuantity,
                quantityDifference,
                quantityToBuy,
                quantityToSell,
                estimatedTradeAmount,
                suggestedAction,
                OffsetDateTime.now(
                        ZoneOffset.UTC
                )
        );
    }

    private BigDecimal calculateAvailableCash(
            PortfolioEntity portfolio
    ) {
        BigDecimal currentValue =
                zeroIfNull(
                        portfolio.getCurrentValue()
                );

        BigDecimal totalMarketValue =
                zeroIfNull(
                        portfolioHoldingRepository
                                .sumMarketValueByPortfolioId(
                                        portfolio.getId()
                                )
                );

        BigDecimal availableCash =
                currentValue.subtract(
                        totalMarketValue
                );

        if (
                availableCash.signum() < 0
        ) {
            throw new InvalidTransactionException(
                    "Portfolio available cash must not be negative"
            );
        }

        return availableCash.setScale(
                MONEY_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private void validateExecutablePreview(
            AllocationPurchasePreviewResponse preview
    ) {
        if (
                "HOLD".equals(
                        preview.suggestedAction()
                )
        ) {
            throw new InvalidTransactionException(
                    "The selected asset is already at "
                            + "its requested target allocation"
            );
        }

        BigDecimal tradedQuantity =
                resolveTradedQuantity(
                        preview
                );

        if (
                tradedQuantity.compareTo(
                        ZERO
                ) <= 0
        ) {
            throw new InvalidTransactionException(
                    "Trade quantity must be greater than zero"
            );
        }

        if (
                preview
                        .estimatedTradeAmount()
                        .compareTo(
                                ZERO
                        ) <= 0
        ) {
            throw new InvalidTransactionException(
                    "Trade amount must be greater than zero"
            );
        }

        if (
                "SELL".equals(
                        preview.suggestedAction()
                )
                        &&
                preview
                        .quantityToSell()
                        .compareTo(
                                preview.existingQuantity()
                        ) > 0
        ) {
            throw new InvalidTransactionException(
                    "Sell quantity exceeds "
                            + "the available asset quantity"
            );
        }
    }

    private TransactionType
    resolveTransactionType(
            String suggestedAction
    ) {
        if (
                "BUY".equals(
                        suggestedAction
                )
        ) {
            return TransactionType.BUY;
        }

        if (
                "SELL".equals(
                        suggestedAction
                )
        ) {
            return TransactionType.SELL;
        }

        throw new InvalidTransactionException(
                "Unsupported allocation action: "
                        + suggestedAction
        );
    }

    private BigDecimal resolveTradedQuantity(
            AllocationPurchasePreviewResponse preview
    ) {
        if (
                "BUY".equals(
                        preview.suggestedAction()
                )
        ) {
            return preview.quantityToBuy();
        }

        if (
                "SELL".equals(
                        preview.suggestedAction()
                )
        ) {
            return preview.quantityToSell();
        }

        return ZERO.setScale(
                QUANTITY_SCALE,
                RoundingMode.DOWN
        );
    }

    private BigDecimal calculateCashImpact(
            TransactionType transactionType,
            BigDecimal tradeAmount,
            BigDecimal fee
    ) {
        if (
                transactionType ==
                        TransactionType.BUY
        ) {
            return tradeAmount
                    .add(
                            fee
                    )
                    .negate()
                    .setScale(
                            MONEY_SCALE,
                            RoundingMode.HALF_UP
                    );
        }

        if (
                transactionType ==
                        TransactionType.SELL
        ) {
            return tradeAmount
                    .subtract(
                            fee
                    )
                    .setScale(
                            MONEY_SCALE,
                            RoundingMode.HALF_UP
                    );
        }

        throw new InvalidTransactionException(
                "Unsupported allocation transaction type: "
                        + transactionType
        );
    }

    private BigDecimal normalizeFee(
            BigDecimal fee
    ) {
        if (
                fee == null
        ) {
            return ZERO.setScale(
                    MONEY_SCALE,
                    RoundingMode.HALF_UP
            );
        }

        if (
                fee.signum() < 0
        ) {
            throw new InvalidTransactionException(
                    "Trade fee must not be negative"
            );
        }

        return fee.setScale(
                MONEY_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private String createTransactionNotes(
            AllocationPurchasePreviewResponse preview
    ) {
        return (
                "Automatic allocation "
                        + preview.suggestedAction()
                        + " for "
                        + preview.targetWeightPercent()
                        + "% target portfolio weight"
        );
    }

    private BigDecimal
    calculateWeightAssignedToOtherAssets(
            List<PortfolioHoldingEntity> holdings,
            UUID selectedAssetId
    ) {
        return holdings
                .stream()
                .filter(
                        holding ->
                                holding.getAsset() != null
                                        &&
                                holding.getAsset()
                                        .getId() != null
                )
                .filter(
                        holding ->
                                !selectedAssetId.equals(
                                        holding
                                                .getAsset()
                                                .getId()
                                )
                )
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
                )
                .setScale(
                        WEIGHT_SCALE,
                        RoundingMode.HALF_UP
                );
    }

    private PortfolioHoldingEntity
    findExistingHolding(
            List<PortfolioHoldingEntity> holdings,
            UUID assetId
    ) {
        return holdings
                .stream()
                .filter(
                        holding ->
                                holding.getAsset() != null
                                        &&
                                assetId.equals(
                                        holding
                                                .getAsset()
                                                .getId()
                                )
                )
                .findFirst()
                .orElse(
                        null
                );
    }

    private BigDecimal normalizeTargetWeight(
            BigDecimal targetWeight
    ) {
        if (
                targetWeight == null
                        ||
                targetWeight.compareTo(
                        ZERO
                ) < 0
        ) {
            throw new InvalidTransactionException(
                    "Target weight percent must not be negative"
            );
        }

        if (
                targetWeight.compareTo(
                        ONE_HUNDRED
                ) > 0
        ) {
            throw new InvalidTransactionException(
                    "Target weight percent must not exceed 100"
            );
        }

        return targetWeight.setScale(
                WEIGHT_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal requirePositivePrice(
            BigDecimal price
    ) {
        if (
                price == null
                        ||
                price.compareTo(
                        ZERO
                ) <= 0
        ) {
            throw new InvalidTransactionException(
                    "A positive current market price "
                            + "is required to calculate "
                            + "the allocation trade"
            );
        }

        return price;
    }

    private BigDecimal requirePositivePortfolioValue(
            BigDecimal value
    ) {
        if (
                value == null
                        ||
                value.compareTo(
                        ZERO
                ) <= 0
        ) {
            throw new InvalidTransactionException(
                    "Portfolio current value "
                            + "must be greater than zero"
            );
        }

        return value;
    }

    private String resolveSuggestedAction(
            BigDecimal quantityDifference
    ) {
        if (
                quantityDifference.signum() > 0
        ) {
            return "BUY";
        }

        if (
                quantityDifference.signum() < 0
        ) {
            return "SELL";
        }

        return "HOLD";
    }

    private BigDecimal zeroIfNull(
            BigDecimal value
    ) {
        return value == null
                ? ZERO
                : value;
    }
}