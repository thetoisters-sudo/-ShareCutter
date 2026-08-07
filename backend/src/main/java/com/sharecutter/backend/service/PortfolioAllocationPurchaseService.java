package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.PortfolioHoldingEntity;
import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
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

    private final PortfolioService portfolioService;

    private final AssetService assetService;

    private final PortfolioHoldingRepository
            portfolioHoldingRepository;

    private final MarketDataService marketDataService;

    private final TransactionService transactionService;

    public PortfolioAllocationPurchaseService(
            PortfolioService portfolioService,
            AssetService assetService,
            PortfolioHoldingRepository
                    portfolioHoldingRepository,
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
                    "Allocation purchase preview request "
                            + "must not be null"
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
                    "Allocation purchase execution request "
                            + "must not be null"
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

        BigDecimal totalCashRequired =
                preview
                        .estimatedPurchaseAmount()
                        .add(
                                normalizedFee
                        )
                        .setScale(
                                MONEY_SCALE,
                                RoundingMode.HALF_UP
                        );

        validateAvailableCash(
                availableCashBefore,
                totalCashRequired
        );

        OffsetDateTime executionTime =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                );

        TransactionEntity transaction =
                transactionService.createTransaction(
                        userId,
                        portfolioId,
                        asset.getId(),
                        TransactionType.BUY,
                        preview.quantityToBuy(),
                        preview.currentPrice(),
                        normalizedFee,
                        preview.estimatedPurchaseAmount(),
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
                        .orElseThrow(
                                () ->
                                        new InvalidTransactionException(
                                                "Portfolio holding was not "
                                                        + "created after the "
                                                        + "purchase transaction"
                                        )
                        );

        holding.updateTargetWeightPercent(
                preview.targetWeightPercent()
        );

        portfolioHoldingRepository.save(
                holding
        );

        BigDecimal availableCashAfter =
                availableCashBefore
                        .subtract(
                                totalCashRequired
                        )
                        .setScale(
                                MONEY_SCALE,
                                RoundingMode.HALF_UP
                        );

        return new AllocationPurchaseExecutionResponse(
                portfolio.getId(),
                portfolio.getName(),
                asset.getId(),
                asset.getSymbol(),
                asset.getDisplayName(),
                transaction.getId(),
                preview.targetWeightPercent(),
                preview.currentPrice(),
                preview.quantityToBuy(),
                preview.estimatedPurchaseAmount(),
                normalizedFee,
                totalCashRequired,
                availableCashBefore,
                availableCashAfter,
                preview.remainingAssignableWeightPercent(),
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

        validatePortfolioCreationMethod(
                portfolio
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

        validateTotalTargetWeight(
                weightAssignedToOtherAssets,
                normalizedTargetWeight
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
                marketDataService.getLatestPrice(
                        asset.getSymbol(),
                        asset.getExchange()
                );

        BigDecimal currentPrice =
                requirePositivePrice(
                        marketPrice.price()
                );

        BigDecimal initialValue =
                requirePositivePortfolioValue(
                        portfolio.getInitialValue()
                );

        BigDecimal targetMarketValue =
                initialValue
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
                targetQuantity.subtract(
                        existingQuantity
                );

        BigDecimal quantityToBuy =
                quantityDifference.signum() > 0
                        ? quantityDifference.setScale(
                                QUANTITY_SCALE,
                                RoundingMode.DOWN
                        )
                        : ZERO.setScale(
                                QUANTITY_SCALE,
                                RoundingMode.DOWN
                        );

        BigDecimal estimatedPurchaseAmount =
                quantityToBuy
                        .multiply(
                                currentPrice
                        )
                        .setScale(
                                MONEY_SCALE,
                                RoundingMode.HALF_UP
                        );

        BigDecimal totalAssignedWeight =
                weightAssignedToOtherAssets.add(
                        normalizedTargetWeight
                );

        BigDecimal remainingAssignableWeight =
                ONE_HUNDRED
                        .subtract(
                                totalAssignedWeight
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
                initialValue.setScale(
                        MONEY_SCALE,
                        RoundingMode.HALF_UP
                ),
                currentPrice.setScale(
                        MONEY_SCALE,
                        RoundingMode.HALF_UP
                ),
                normalizedTargetWeight,
                weightAssignedToOtherAssets,
                remainingAssignableWeight,
                targetMarketValue,
                existingQuantity.setScale(
                        QUANTITY_SCALE,
                        RoundingMode.HALF_UP
                ),
                targetQuantity,
                quantityToBuy,
                estimatedPurchaseAmount,
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

        if (availableCash.signum() < 0) {
            throw new InvalidTransactionException(
                    "Portfolio available cash "
                            + "must not be negative"
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
                !"BUY".equals(
                        preview.suggestedAction()
                )
        ) {
            throw new InvalidTransactionException(
                    "The selected target weight does not "
                            + "require an additional purchase. "
                            + "Suggested action: "
                            + preview.suggestedAction()
            );
        }

        if (
                preview.quantityToBuy()
                        .compareTo(
                                ZERO
                        ) <= 0
        ) {
            throw new InvalidTransactionException(
                    "Purchase quantity must be greater than zero"
            );
        }

        if (
                preview.estimatedPurchaseAmount()
                        .compareTo(
                                ZERO
                        ) <= 0
        ) {
            throw new InvalidTransactionException(
                    "Purchase amount must be greater than zero"
            );
        }
    }

    private void validateAvailableCash(
            BigDecimal availableCash,
            BigDecimal requiredCash
    ) {
        if (
                requiredCash.compareTo(
                        availableCash
                ) > 0
        ) {
            throw new InvalidTransactionException(
                    "Insufficient portfolio cash. "
                            + "Available cash: "
                            + availableCash
                            + ", required cash: "
                            + requiredCash
            );
        }
    }

    private BigDecimal normalizeFee(
            BigDecimal fee
    ) {
        if (fee == null) {
            return ZERO.setScale(
                    MONEY_SCALE,
                    RoundingMode.HALF_UP
            );
        }

        if (fee.signum() < 0) {
            throw new InvalidTransactionException(
                    "Purchase fee must not be negative"
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
                "Automatic allocation purchase for "
                        + preview.targetWeightPercent()
                        + "% target portfolio weight"
        );
    }

    private void validatePortfolioCreationMethod(
            PortfolioEntity portfolio
    ) {
        if (
                portfolio.getCreationMethod()
                        != PortfolioCreationMethod.BY_AMOUNT
        ) {
            throw new InvalidTransactionException(
                    "Percentage-based allocation purchases "
                            + "are available only for portfolios "
                            + "created by amount"
            );
        }
    }

    private BigDecimal calculateWeightAssignedToOtherAssets(
            List<PortfolioHoldingEntity> holdings,
            UUID selectedAssetId
    ) {
        return holdings
                .stream()
                .filter(
                        holding ->
                                holding.getAsset() != null
                                        && holding.getAsset()
                                        .getId() != null
                )
                .filter(
                        holding ->
                                !selectedAssetId.equals(
                                        holding.getAsset()
                                                .getId()
                                )
                )
                .map(
                        PortfolioHoldingEntity::
                                getTargetWeightPercent
                )
                .map(this::zeroIfNull)
                .reduce(
                        ZERO,
                        BigDecimal::add
                )
                .setScale(
                        WEIGHT_SCALE,
                        RoundingMode.HALF_UP
                );
    }

    private PortfolioHoldingEntity findExistingHolding(
            List<PortfolioHoldingEntity> holdings,
            UUID assetId
    ) {
        return holdings
                .stream()
                .filter(
                        holding ->
                                holding.getAsset() != null
                                        && assetId.equals(
                                        holding.getAsset()
                                                .getId()
                                )
                )
                .findFirst()
                .orElse(null);
    }

    private void validateTotalTargetWeight(
            BigDecimal weightAssignedToOtherAssets,
            BigDecimal requestedTargetWeight
    ) {
        BigDecimal totalTargetWeight =
                weightAssignedToOtherAssets.add(
                        requestedTargetWeight
                );

        if (
                totalTargetWeight.compareTo(
                        ONE_HUNDRED
                ) > 0
        ) {
            BigDecimal remainingWeight =
                    ONE_HUNDRED
                            .subtract(
                                    weightAssignedToOtherAssets
                            )
                            .max(ZERO)
                            .setScale(
                                    WEIGHT_SCALE,
                                    RoundingMode.HALF_UP
                            );

            throw new InvalidTransactionException(
                    "Requested target weight exceeds "
                            + "the remaining portfolio allocation. "
                            + "Remaining weight: "
                            + remainingWeight
                            + "%"
            );
        }
    }

    private BigDecimal normalizeTargetWeight(
            BigDecimal targetWeight
    ) {
        if (
                targetWeight == null
                        || targetWeight.compareTo(
                        ZERO
                ) <= 0
        ) {
            throw new InvalidTransactionException(
                    "Target weight percent "
                            + "must be greater than zero"
            );
        }

        if (
                targetWeight.compareTo(
                        ONE_HUNDRED
                ) > 0
        ) {
            throw new InvalidTransactionException(
                    "Target weight percent "
                            + "must not exceed 100"
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
                        || price.compareTo(
                        ZERO
                ) <= 0
        ) {
            throw new InvalidTransactionException(
                    "A positive current market price "
                            + "is required to calculate "
                            + "the purchase quantity"
            );
        }

        return price;
    }

    private BigDecimal requirePositivePortfolioValue(
            BigDecimal value
    ) {
        if (
                value == null
                        || value.compareTo(
                        ZERO
                ) <= 0
        ) {
            throw new InvalidTransactionException(
                    "Portfolio initial value "
                            + "must be greater than zero"
            );
        }

        return value;
    }

    private String resolveSuggestedAction(
            BigDecimal quantityDifference
    ) {
        if (quantityDifference.signum() > 0) {
            return "BUY";
        }

        if (quantityDifference.signum() < 0) {
            return "SELL_REQUIRED";
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