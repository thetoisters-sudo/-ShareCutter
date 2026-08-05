package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.PortfolioHoldingEntity;
import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.domain.enums.TransactionType;
import com.sharecutter.backend.exception.InvalidTransactionException;
import com.sharecutter.backend.repository.PortfolioHoldingRepository;
import com.sharecutter.backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PortfolioHoldingCalculationService {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private static final int CALCULATION_SCALE = 8;

    private final PortfolioHoldingRepository
            portfolioHoldingRepository;

    private final TransactionRepository
            transactionRepository;

    private final PortfolioService
            portfolioService;

    public PortfolioHoldingCalculationService(
            PortfolioHoldingRepository portfolioHoldingRepository,
            TransactionRepository transactionRepository,
            PortfolioService portfolioService
    ) {
        this.portfolioHoldingRepository =
                portfolioHoldingRepository;

        this.transactionRepository =
                transactionRepository;

        this.portfolioService =
                portfolioService;
    }

    @Transactional
    public PortfolioHoldingEntity recalculateAssetHolding(
            UUID userId,
            UUID portfolioId,
            AssetEntity asset
    ) {
        if (asset == null) {
            throw new IllegalArgumentException(
                    "Holding asset must not be null"
            );
        }

        PortfolioEntity portfolio =
                portfolioService.getPortfolio(
                        userId,
                        portfolioId
                );

        validateAssetPortfolio(
                portfolioId,
                asset
        );

        if (asset.isDeleted()) {
            PortfolioHoldingEntity deletedHolding =
                    deactivateAssetHoldings(
                            portfolioId,
                            asset.getId()
                    );

            recalculatePortfolio(
                    userId,
                    portfolioId
            );

            if (deletedHolding != null) {
                return deletedHolding;
            }

            PortfolioHoldingEntity emptyHolding =
                    new PortfolioHoldingEntity(
                            portfolio,
                            asset
                    );

            emptyHolding.softDelete();

            return emptyHolding;
        }

        List<TransactionEntity> transactions =
                getOrderedAssetTransactions(
                        asset.getId()
                );

        HoldingCalculationState state =
                calculateHoldingState(
                        transactions,
                        asset
                );

        PortfolioHoldingEntity holding =
                portfolioHoldingRepository
                        .findByPortfolioIdAndAssetIdAndDeletedAtIsNull(
                                portfolioId,
                                asset.getId()
                        )
                        .orElseGet(
                                () ->
                                        new PortfolioHoldingEntity(
                                                portfolio,
                                                asset
                                        )
                        );

        holding.replaceCalculatedState(
                state.quantity(),
                state.averageCost(),
                state.currentPrice(),
                state.realizedProfit()
        );

        PortfolioHoldingEntity savedHolding =
                portfolioHoldingRepository.save(
                        holding
                );

        recalculatePortfolio(
                userId,
                portfolioId
        );

        return savedHolding;
    }

    @Transactional
    public PortfolioEntity rebuildPortfolioState(
            UUID userId,
            UUID portfolioId
    ) {
        portfolioService.getPortfolio(
                userId,
                portfolioId
        );

        List<TransactionEntity> transactions =
                transactionRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                                portfolioId
                        );

        List<PortfolioHoldingEntity> existingHoldings =
                portfolioHoldingRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByAssetSymbolAsc(
                                portfolioId
                        );

        Map<UUID, AssetEntity> affectedAssets =
                new LinkedHashMap<>();

        List<PortfolioHoldingEntity>
                holdingsToDeactivate =
                new ArrayList<>();

        for (
                PortfolioHoldingEntity holding
                : existingHoldings
        ) {
            AssetEntity asset =
                    holding.getAsset();

            if (
                    asset == null
                            || asset.getId() == null
            ) {
                continue;
            }

            if (asset.isDeleted()) {
                holding.softDelete();

                holdingsToDeactivate.add(
                        holding
                );

                continue;
            }

            affectedAssets.put(
                    asset.getId(),
                    asset
            );
        }

        if (!holdingsToDeactivate.isEmpty()) {
            portfolioHoldingRepository.saveAll(
                    holdingsToDeactivate
            );
        }

        for (
                TransactionEntity transaction
                : transactions
        ) {
            AssetEntity asset =
                    transaction.getAsset();

            if (
                    asset == null
                            || asset.getId() == null
                            || asset.isDeleted()
            ) {
                continue;
            }

            affectedAssets.put(
                    asset.getId(),
                    asset
            );
        }

        for (
                AssetEntity asset
                : affectedAssets.values()
        ) {
            recalculateAssetHolding(
                    userId,
                    portfolioId,
                    asset
            );
        }

        return recalculatePortfolio(
                userId,
                portfolioId
        );
    }

    @Transactional
    public PortfolioEntity recalculatePortfolio(
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
                        );

        List<PortfolioHoldingEntity>
                invalidHoldings =
                holdings
                        .stream()
                        .filter(
                                this::belongsToDeletedAsset
                        )
                        .toList();

        for (
                PortfolioHoldingEntity invalidHolding
                : invalidHoldings
        ) {
            invalidHolding.softDelete();
        }

        if (!invalidHoldings.isEmpty()) {
            portfolioHoldingRepository.saveAll(
                    invalidHoldings
            );
        }

        List<PortfolioHoldingEntity>
                activeHoldings =
                holdings
                        .stream()
                        .filter(
                                holding ->
                                        !holding.isDeleted()
                        )
                        .filter(
                                holding ->
                                        !belongsToDeletedAsset(
                                                holding
                                        )
                        )
                        .toList();

        BigDecimal totalMarketValue =
                activeHoldings
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

        BigDecimal totalRealizedProfit =
                activeHoldings
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
                activeHoldings
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

        BigDecimal cashBalance =
                calculatePortfolioCashBalance(
                        portfolio
                );

        BigDecimal currentValue =
                cashBalance.add(
                        totalMarketValue
                );

        if (currentValue.signum() < 0) {
            throw new InvalidTransactionException(
                    "Portfolio calculated value must not be negative"
            );
        }

        portfolio.updateTotalRealizedProfit(
                totalRealizedProfit
        );

        portfolio.updateTotalUnrealizedProfit(
                totalUnrealizedProfit
        );

        portfolio.updateCurrentValue(
                currentValue
        );

        return portfolio;
    }

    private List<TransactionEntity>
    getOrderedAssetTransactions(
            UUID assetId
    ) {
        return transactionRepository
                .findAllByAssetIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        assetId
                )
                .stream()
                .filter(
                        transaction ->
                                isHoldingTransaction(
                                        transaction
                                                .getTransactionType()
                                )
                )
                .sorted(
                        Comparator
                                .comparing(
                                        TransactionEntity::
                                                getExecutedAt
                                )
                                .thenComparing(
                                        TransactionEntity::
                                                getCreatedAt,
                                        Comparator.nullsLast(
                                                Comparator
                                                        .naturalOrder()
                                        )
                                )
                )
                .toList();
    }

    private HoldingCalculationState
    calculateHoldingState(
            List<TransactionEntity> transactions,
            AssetEntity asset
    ) {
        BigDecimal quantity =
                ZERO;

        BigDecimal totalCost =
                ZERO;

        BigDecimal currentPrice =
                ZERO;

        BigDecimal realizedProfit =
                ZERO;

        for (
                TransactionEntity transaction
                : transactions
        ) {
            TransactionType transactionType =
                    transaction
                            .getTransactionType();

            if (
                    transactionType
                            == TransactionType.BUY
            ) {
                BigDecimal transactionQuantity =
                        requirePositive(
                                transaction
                                        .getQuantity(),
                                "BUY quantity"
                        );

                BigDecimal unitPrice =
                        requireNonNegative(
                                transaction
                                        .getUnitPrice(),
                                "BUY unit price"
                        );

                BigDecimal fee =
                        zeroIfNull(
                                transaction
                                        .getFee()
                        );

                BigDecimal purchaseCost =
                        transactionQuantity
                                .multiply(
                                        unitPrice
                                )
                                .add(
                                        fee
                                );

                quantity =
                        quantity.add(
                                transactionQuantity
                        );

                totalCost =
                        totalCost.add(
                                purchaseCost
                        );

                currentPrice =
                        unitPrice;
            }

            if (
                    transactionType
                            == TransactionType.SELL
            ) {
                BigDecimal transactionQuantity =
                        requirePositive(
                                transaction
                                        .getQuantity(),
                                "SELL quantity"
                        );

                BigDecimal unitPrice =
                        requireNonNegative(
                                transaction
                                        .getUnitPrice(),
                                "SELL unit price"
                        );

                if (
                        transactionQuantity
                                .compareTo(
                                        quantity
                                ) > 0
                ) {
                    throw new InvalidTransactionException(
                            "SELL transaction for asset "
                                    + asset.getSymbol()
                                    + " exceeds the available quantity"
                    );
                }

                BigDecimal averageCost =
                        calculateAverageCost(
                                totalCost,
                                quantity
                        );

                BigDecimal soldCostBasis =
                        averageCost.multiply(
                                transactionQuantity
                        );

                BigDecimal saleProceeds =
                        transactionQuantity.multiply(
                                unitPrice
                        );

                BigDecimal fee =
                        zeroIfNull(
                                transaction
                                        .getFee()
                        );

                BigDecimal transactionProfit =
                        saleProceeds
                                .subtract(
                                        soldCostBasis
                                )
                                .subtract(
                                        fee
                                );

                quantity =
                        quantity.subtract(
                                transactionQuantity
                        );

                totalCost =
                        totalCost.subtract(
                                soldCostBasis
                        );

                realizedProfit =
                        realizedProfit.add(
                                transactionProfit
                        );

                currentPrice =
                        unitPrice;

                if (
                        quantity.compareTo(
                                ZERO
                        ) == 0
                ) {
                    quantity =
                            ZERO;

                    totalCost =
                            ZERO;
                }
            }

            if (
                    transactionType
                            == TransactionType.DIVIDEND
            ) {
                BigDecimal dividendAmount =
                        zeroIfNull(
                                transaction
                                        .getTotalAmount()
                        );

                BigDecimal fee =
                        zeroIfNull(
                                transaction
                                        .getFee()
                        );

                realizedProfit =
                        realizedProfit.add(
                                dividendAmount.subtract(
                                        fee
                                )
                        );
            }
        }

        BigDecimal averageCost =
                calculateAverageCost(
                        totalCost,
                        quantity
                );

        return new HoldingCalculationState(
                normalize(
                        quantity
                ),
                normalize(
                        averageCost
                ),
                normalize(
                        currentPrice
                ),
                normalize(
                        realizedProfit
                )
        );
    }

    private BigDecimal calculatePortfolioCashBalance(
            PortfolioEntity portfolio
    ) {
        BigDecimal cashBalance =
                zeroIfNull(
                        portfolio
                                .getInitialValue()
                );

        List<TransactionEntity> transactions =
                transactionRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                                portfolio.getId()
                        );

        for (
                TransactionEntity transaction
                : transactions
        ) {
            AssetEntity transactionAsset =
                    transaction.getAsset();

            if (
                    transactionAsset != null
                            && transactionAsset.isDeleted()
            ) {
                continue;
            }

            BigDecimal totalAmount =
                    resolveEffectiveTotalAmount(
                            transaction
                    );

            BigDecimal fee =
                    zeroIfNull(
                            transaction
                                    .getFee()
                    );

            switch (
                    transaction
                            .getTransactionType()
            ) {
                case BUY ->
                        cashBalance =
                                cashBalance
                                        .subtract(
                                                totalAmount
                                        )
                                        .subtract(
                                                fee
                                        );

                case SELL ->
                        cashBalance =
                                cashBalance
                                        .add(
                                                totalAmount
                                        )
                                        .subtract(
                                                fee
                                        );

                case DIVIDEND ->
                        cashBalance =
                                cashBalance
                                        .add(
                                                totalAmount
                                        )
                                        .subtract(
                                                fee
                                        );

                case DEPOSIT ->
                        cashBalance =
                                cashBalance.add(
                                        totalAmount
                                );

                case WITHDRAWAL ->
                        cashBalance =
                                cashBalance.subtract(
                                        totalAmount
                                );

                case FEE ->
                        cashBalance =
                                cashBalance.subtract(
                                        totalAmount
                                );
            }
        }

        return normalize(
                cashBalance
        );
    }

    private BigDecimal resolveEffectiveTotalAmount(
            TransactionEntity transaction
    ) {
        if (
                transaction.getTransactionType()
                        == TransactionType.BUY
                        || transaction.getTransactionType()
                        == TransactionType.SELL
        ) {
            BigDecimal quantity =
                    requirePositive(
                            transaction
                                    .getQuantity(),
                            transaction
                                    .getTransactionType()
                                    + " quantity"
                    );

            BigDecimal unitPrice =
                    requireNonNegative(
                            transaction
                                    .getUnitPrice(),
                            transaction
                                    .getTransactionType()
                                    + " unit price"
                    );

            return normalize(
                    quantity.multiply(
                            unitPrice
                    )
            );
        }

        return normalize(
                transaction.getTotalAmount()
        );
    }

    private PortfolioHoldingEntity
    deactivateAssetHoldings(
            UUID portfolioId,
            UUID assetId
    ) {
        List<PortfolioHoldingEntity>
                activeHoldings =
                portfolioHoldingRepository
                        .findAllByAssetIdAndDeletedAtIsNull(
                                assetId
                        )
                        .stream()
                        .filter(
                                holding ->
                                        holding.getPortfolio()
                                                        != null
                                                && portfolioId.equals(
                                                        holding
                                                                .getPortfolio()
                                                                .getId()
                                                )
                        )
                        .toList();

        for (
                PortfolioHoldingEntity holding
                : activeHoldings
        ) {
            holding.softDelete();
        }

        if (!activeHoldings.isEmpty()) {
            portfolioHoldingRepository.saveAll(
                    activeHoldings
            );

            return activeHoldings.getFirst();
        }

        return null;
    }

    private boolean belongsToDeletedAsset(
            PortfolioHoldingEntity holding
    ) {
        return holding.getAsset() == null
                || holding.getAsset().isDeleted();
    }

    private BigDecimal calculateAverageCost(
            BigDecimal totalCost,
            BigDecimal quantity
    ) {
        if (
                quantity == null
                        || quantity.compareTo(
                                ZERO
                        ) == 0
        ) {
            return ZERO;
        }

        return zeroIfNull(
                totalCost
        ).divide(
                quantity,
                CALCULATION_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private boolean isHoldingTransaction(
            TransactionType transactionType
    ) {
        return transactionType
                == TransactionType.BUY
                || transactionType
                == TransactionType.SELL
                || transactionType
                == TransactionType.DIVIDEND;
    }

    private void validateAssetPortfolio(
            UUID portfolioId,
            AssetEntity asset
    ) {
        if (
                asset.getPortfolio() == null
                        || asset.getPortfolio()
                        .getId() == null
                        || !portfolioId.equals(
                                asset.getPortfolio()
                                        .getId()
                        )
        ) {
            throw new InvalidTransactionException(
                    "Asset does not belong to the selected portfolio"
            );
        }
    }

    private BigDecimal requirePositive(
            BigDecimal value,
            String fieldName
    ) {
        if (
                value == null
                        || value.compareTo(
                                ZERO
                        ) <= 0
        ) {
            throw new InvalidTransactionException(
                    fieldName
                            + " must be greater than zero"
            );
        }

        return value;
    }

    private BigDecimal requireNonNegative(
            BigDecimal value,
            String fieldName
    ) {
        if (
                value == null
                        || value.compareTo(
                                ZERO
                        ) < 0
        ) {
            throw new InvalidTransactionException(
                    fieldName
                            + " must not be negative"
            );
        }

        return value;
    }

    private BigDecimal zeroIfNull(
            BigDecimal value
    ) {
        return value == null
                ? ZERO
                : value;
    }

    private BigDecimal normalize(
            BigDecimal value
    ) {
        return zeroIfNull(
                value
        ).setScale(
                CALCULATION_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private record HoldingCalculationState(
            BigDecimal quantity,
            BigDecimal averageCost,
            BigDecimal currentPrice,
            BigDecimal realizedProfit
    ) {
    }
}