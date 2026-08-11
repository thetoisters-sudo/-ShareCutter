package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.PortfolioHoldingEntity;
import com.sharecutter.backend.domain.enums.AssetType;
import com.sharecutter.backend.exception.AssetAlreadyExistsException;
import com.sharecutter.backend.exception.AssetInUseException;
import com.sharecutter.backend.exception.AssetNotFoundException;
import com.sharecutter.backend.repository.AssetRepository;
import com.sharecutter.backend.repository.PortfolioHoldingRepository;
import com.sharecutter.backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AssetService {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private final AssetRepository assetRepository;

    private final PortfolioHoldingRepository
            portfolioHoldingRepository;

    private final TransactionRepository
            transactionRepository;

    private final PortfolioService portfolioService;

    private final PortfolioHoldingCalculationService
            portfolioHoldingCalculationService;

    public AssetService(
            AssetRepository assetRepository,
            PortfolioHoldingRepository
                    portfolioHoldingRepository,
            TransactionRepository
                    transactionRepository,
            PortfolioService portfolioService,
            PortfolioHoldingCalculationService
                    portfolioHoldingCalculationService
    ) {
        this.assetRepository =
                assetRepository;

        this.portfolioHoldingRepository =
                portfolioHoldingRepository;

        this.transactionRepository =
                transactionRepository;

        this.portfolioService =
                portfolioService;

        this.portfolioHoldingCalculationService =
                portfolioHoldingCalculationService;
    }

    @Transactional
    public AssetEntity createAsset(
            UUID userId,
            UUID portfolioId,
            String symbol,
            String displayName,
            AssetType assetType,
            String currency,
            String isin,
            String exchange,
            String notes
    ) {
        PortfolioEntity portfolio =
                portfolioService.getPortfolio(
                        userId,
                        portfolioId
                );

        String normalizedSymbol =
                normalizeSymbol(
                        symbol
                );

        validateUniqueSymbol(
                portfolioId,
                normalizedSymbol,
                null
        );

        AssetEntity asset =
                new AssetEntity(
                        portfolio,
                        normalizedSymbol,
                        displayName,
                        assetType,
                        currency,
                        isin,
                        exchange,
                        notes
                );

        return assetRepository.save(
                asset
        );
    }

    public AssetEntity getAsset(
            UUID userId,
            UUID portfolioId,
            UUID assetId
    ) {
        portfolioService.getPortfolio(
                userId,
                portfolioId
        );

        return assetRepository
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        assetId,
                        portfolioId
                )
                .orElseThrow(
                        () ->
                                new AssetNotFoundException(
                                        assetId
                                )
                );
    }

    public List<AssetEntity> getPortfolioAssets(
            UUID userId,
            UUID portfolioId
    ) {
        portfolioService.getPortfolio(
                userId,
                portfolioId
        );

        return assetRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                        portfolioId
                );
    }

    @Transactional
    public AssetEntity updateAsset(
            UUID userId,
            UUID portfolioId,
            UUID assetId,
            String symbol,
            String displayName,
            AssetType assetType,
            String currency,
            String isin,
            String exchange,
            String notes
    ) {
        AssetEntity asset =
                getAsset(
                        userId,
                        portfolioId,
                        assetId
                );

        String normalizedSymbol =
                normalizeSymbol(
                        symbol
                );

        if (
                !asset.getSymbol()
                        .equalsIgnoreCase(
                                normalizedSymbol
                        )
        ) {
            validateUniqueSymbol(
                    portfolioId,
                    normalizedSymbol,
                    assetId
            );
        }

        asset.setSymbol(
                normalizedSymbol
        );

        asset.setDisplayName(
                displayName
        );

        asset.setAssetType(
                assetType
        );

        asset.setCurrency(
                currency
        );

        asset.setIsin(
                isin
        );

        asset.setExchange(
                exchange
        );

        asset.setNotes(
                notes
        );

        return assetRepository.save(
                asset
        );
    }

    @Transactional
    public void deleteAsset(
            UUID userId,
            UUID portfolioId,
            UUID assetId
    ) {
        AssetEntity asset =
                getAsset(
                        userId,
                        portfolioId,
                        assetId
                );

        if (
                transactionRepository
                        .existsByAssetIdAndDeletedAtIsNull(
                                assetId
                        )
        ) {
            throw new AssetInUseException(
                    assetId,
                    "active transactions still exist"
            );
        }

        List<PortfolioHoldingEntity>
                activeHoldings =
                portfolioHoldingRepository
                        .findAllByAssetIdAndDeletedAtIsNull(
                                assetId
                        );

        boolean hasPositiveHolding =
                activeHoldings
                        .stream()
                        .map(
                                PortfolioHoldingEntity::getQuantity
                        )
                        .map(this::zeroIfNull)
                        .anyMatch(
                                quantity ->
                                        quantity.compareTo(
                                                ZERO
                                        ) > 0
                        );

        if (hasPositiveHolding) {
            throw new AssetInUseException(
                    assetId,
                    "the portfolio still holds a positive quantity"
            );
        }

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
        }

        asset.softDelete();

        assetRepository.save(
                asset
        );

        portfolioHoldingCalculationService
                .recalculatePortfolio(
                        userId,
                        portfolioId
                );
    }

    private void validateUniqueSymbol(
            UUID portfolioId,
            String symbol,
            UUID excludedAssetId
    ) {
        assetRepository
                .findByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                        portfolioId,
                        symbol
                )
                .filter(
                        existingAsset ->
                                excludedAssetId == null
                                        || !existingAsset
                                        .getId()
                                        .equals(
                                                excludedAssetId
                                        )
                )
                .ifPresent(
                        existingAsset -> {
                            throw new AssetAlreadyExistsException(
                                    portfolioId,
                                    symbol
                            );
                        }
                );
    }

    private BigDecimal zeroIfNull(
            BigDecimal value
    ) {
        return value == null
                ? ZERO
                : value;
    }

    private String normalizeSymbol(
            String symbol
    ) {
        if (
                symbol == null
                        || symbol.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Asset symbol must not be blank"
            );
        }

        return symbol
                .trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }
}