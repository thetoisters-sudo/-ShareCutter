package com.sharecutter.backend.repository;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.AssetType;
import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AssetRepositoryTests {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindActiveAssetById() {
        UserEntity user = saveUser(
                "asset.owner@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Primary Portfolio"
        );

        AssetEntity asset = saveAsset(
                portfolio,
                "AAPL",
                "Apple Inc.",
                AssetType.STOCK,
                "USD",
                "US0378331005",
                "NASDAQ",
                "Long-term holding"
        );

        Optional<AssetEntity> result =
                assetRepository.findByIdAndDeletedAtIsNull(
                        asset.getId()
                );

        assertTrue(result.isPresent());

        AssetEntity foundAsset = result.get();

        assertEquals(
                asset.getId(),
                foundAsset.getId()
        );

        assertEquals(
                portfolio.getId(),
                foundAsset.getPortfolio().getId()
        );

        assertEquals(
                "AAPL",
                foundAsset.getSymbol()
        );

        assertEquals(
                "Apple Inc.",
                foundAsset.getDisplayName()
        );

        assertEquals(
                AssetType.STOCK,
                foundAsset.getAssetType()
        );

        assertEquals(
                "USD",
                foundAsset.getCurrency()
        );

        assertEquals(
                "US0378331005",
                foundAsset.getIsin()
        );

        assertEquals(
                "NASDAQ",
                foundAsset.getExchange()
        );

        assertEquals(
                "Long-term holding",
                foundAsset.getNotes()
        );

        assertFalse(foundAsset.isDeleted());
    }

    @Test
    void shouldFindAssetOnlyInsideItsPortfolio() {
        UserEntity user = saveUser(
                "asset.portfolio.owner@example.com"
        );

        PortfolioEntity firstPortfolio = savePortfolio(
                user,
                "First Portfolio"
        );

        PortfolioEntity secondPortfolio = savePortfolio(
                user,
                "Second Portfolio"
        );

        AssetEntity asset = saveAsset(
                firstPortfolio,
                "MSFT",
                "Microsoft Corporation",
                AssetType.STOCK,
                "USD",
                "US5949181045",
                "NASDAQ",
                null
        );

        Optional<AssetEntity> firstPortfolioResult =
                assetRepository
                        .findByIdAndPortfolioIdAndDeletedAtIsNull(
                                asset.getId(),
                                firstPortfolio.getId()
                        );

        Optional<AssetEntity> secondPortfolioResult =
                assetRepository
                        .findByIdAndPortfolioIdAndDeletedAtIsNull(
                                asset.getId(),
                                secondPortfolio.getId()
                        );

        assertTrue(firstPortfolioResult.isPresent());
        assertTrue(secondPortfolioResult.isEmpty());
    }

    @Test
    void shouldReturnOnlyActiveAssetsForPortfolio() {
        UserEntity user = saveUser(
                "asset.list.owner@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Assets Portfolio"
        );

        AssetEntity firstAsset = saveAsset(
                portfolio,
                "AAPL",
                "Apple Inc.",
                AssetType.STOCK,
                "USD",
                "US0378331005",
                "NASDAQ",
                null
        );

        AssetEntity secondAsset = saveAsset(
                portfolio,
                "BTC",
                "Bitcoin",
                AssetType.CRYPTO,
                "USD",
                null,
                "CRYPTO",
                null
        );

        AssetEntity deletedAsset = saveAsset(
                portfolio,
                "GLD",
                "SPDR Gold Shares",
                AssetType.ETF,
                "USD",
                "US78463V1070",
                "NYSE",
                null
        );

        deletedAsset.softDelete();
        assetRepository.saveAndFlush(deletedAsset);

        List<AssetEntity> result =
                assetRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                                portfolio.getId()
                        );

        assertEquals(2, result.size());

        assertEquals(
                firstAsset.getId(),
                result.get(0).getId()
        );

        assertEquals(
                secondAsset.getId(),
                result.get(1).getId()
        );

        assertTrue(
                result.stream()
                        .noneMatch(AssetEntity::isDeleted)
        );
    }

    @Test
    void shouldFindAssetBySymbolIgnoringCase() {
        UserEntity user = saveUser(
                "asset.symbol.owner@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Symbol Portfolio"
        );

        saveAsset(
                portfolio,
                "NVDA",
                "NVIDIA Corporation",
                AssetType.STOCK,
                "USD",
                "US67066G1040",
                "NASDAQ",
                null
        );

        Optional<AssetEntity> lowercaseResult =
                assetRepository
                        .findByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                                portfolio.getId(),
                                "nvda"
                        );

        Optional<AssetEntity> mixedCaseResult =
                assetRepository
                        .findByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                                portfolio.getId(),
                                "NvDa"
                        );

        Optional<AssetEntity> missingResult =
                assetRepository
                        .findByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                                portfolio.getId(),
                                "TSLA"
                        );

        assertTrue(lowercaseResult.isPresent());
        assertTrue(mixedCaseResult.isPresent());
        assertTrue(missingResult.isEmpty());

        assertEquals(
                "NVDA",
                lowercaseResult.get().getSymbol()
        );
    }

    @Test
    void shouldDetectActiveSymbolIgnoringCase() {
        UserEntity user = saveUser(
                "asset.exists.owner@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Exists Portfolio"
        );

        saveAsset(
                portfolio,
                "VOO",
                "Vanguard S&P 500 ETF",
                AssetType.ETF,
                "USD",
                "US9229083632",
                "NYSE",
                null
        );

        boolean existingSymbol =
                assetRepository
                        .existsByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                                portfolio.getId(),
                                "voo"
                        );

        boolean missingSymbol =
                assetRepository
                        .existsByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                                portfolio.getId(),
                                "QQQ"
                        );

        assertTrue(existingSymbol);
        assertFalse(missingSymbol);
    }

    @Test
    void shouldNotReturnSoftDeletedAsset() {
        UserEntity user = saveUser(
                "asset.deleted.owner@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Deleted Asset Portfolio"
        );

        AssetEntity asset = saveAsset(
                portfolio,
                "AMZN",
                "Amazon.com Inc.",
                AssetType.STOCK,
                "USD",
                "US0231351067",
                "NASDAQ",
                null
        );

        asset.softDelete();
        assetRepository.saveAndFlush(asset);

        Optional<AssetEntity> resultById =
                assetRepository.findByIdAndDeletedAtIsNull(
                        asset.getId()
                );

        Optional<AssetEntity> resultBySymbol =
                assetRepository
                        .findByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                                portfolio.getId(),
                                "AMZN"
                        );

        boolean exists =
                assetRepository
                        .existsByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                                portfolio.getId(),
                                "AMZN"
                        );

        assertTrue(resultById.isEmpty());
        assertTrue(resultBySymbol.isEmpty());
        assertFalse(exists);
    }

    @Test
    void shouldRejectDuplicateActiveSymbolInsideSamePortfolio() {
        UserEntity user = saveUser(
                "asset.duplicate.owner@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Duplicate Portfolio"
        );

        saveAsset(
                portfolio,
                "TSLA",
                "Tesla Inc.",
                AssetType.STOCK,
                "USD",
                "US88160R1014",
                "NASDAQ",
                null
        );

        AssetEntity duplicateAsset = new AssetEntity(
                portfolio,
                "tsla",
                "Tesla Duplicate",
                AssetType.STOCK,
                "USD",
                null,
                "NASDAQ",
                null
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> assetRepository.saveAndFlush(
                        duplicateAsset
                )
        );
    }

    @Test
    void shouldAllowSameSymbolInDifferentPortfolios() {
        UserEntity user = saveUser(
                "asset.multiple.portfolios@example.com"
        );

        PortfolioEntity firstPortfolio = savePortfolio(
                user,
                "Technology Portfolio"
        );

        PortfolioEntity secondPortfolio = savePortfolio(
                user,
                "Retirement Portfolio"
        );

        AssetEntity firstAsset = saveAsset(
                firstPortfolio,
                "META",
                "Meta Platforms Inc.",
                AssetType.STOCK,
                "USD",
                "US30303M1027",
                "NASDAQ",
                null
        );

        AssetEntity secondAsset = saveAsset(
                secondPortfolio,
                "meta",
                "Meta Platforms Inc.",
                AssetType.STOCK,
                "USD",
                "US30303M1027",
                "NASDAQ",
                null
        );

        assertEquals(
                "META",
                firstAsset.getSymbol()
        );

        assertEquals(
                "META",
                secondAsset.getSymbol()
        );

        assertFalse(
                firstAsset.getPortfolio()
                        .getId()
                        .equals(
                                secondAsset.getPortfolio()
                                        .getId()
                        )
        );
    }

    @Test
    void shouldAllowSymbolReuseAfterSoftDelete() {
        UserEntity user = saveUser(
                "asset.reuse.owner@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Reusable Symbol Portfolio"
        );

        AssetEntity originalAsset = saveAsset(
                portfolio,
                "NFLX",
                "Netflix Inc.",
                AssetType.STOCK,
                "USD",
                "US64110L1061",
                "NASDAQ",
                null
        );

        originalAsset.softDelete();
        assetRepository.saveAndFlush(originalAsset);

        AssetEntity replacementAsset = saveAsset(
                portfolio,
                "nflx",
                "Netflix Inc. Replacement",
                AssetType.STOCK,
                "USD",
                "US64110L1061",
                "NASDAQ",
                "Recreated after soft delete"
        );

        assertEquals(
                "NFLX",
                replacementAsset.getSymbol()
        );

        assertFalse(replacementAsset.isDeleted());

        assertTrue(
                assetRepository
                        .findByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                                portfolio.getId(),
                                "NFLX"
                        )
                        .isPresent()
        );
    }

    @Test
    void shouldNormalizeAssetValuesBeforeSaving() {
        UserEntity user = saveUser(
                "asset.normalize.owner@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Normalization Portfolio"
        );

        AssetEntity asset = saveAsset(
                portfolio,
                "  ibm  ",
                "  International Business Machines  ",
                AssetType.STOCK,
                "  usd  ",
                "  us4592001014  ",
                "  NYSE  ",
                "  Legacy technology holding  "
        );

        assertEquals(
                "IBM",
                asset.getSymbol()
        );

        assertEquals(
                "International Business Machines",
                asset.getDisplayName()
        );

        assertEquals(
                "USD",
                asset.getCurrency()
        );

        assertEquals(
                "US4592001014",
                asset.getIsin()
        );

        assertEquals(
                "NYSE",
                asset.getExchange()
        );

        assertEquals(
                "Legacy technology holding",
                asset.getNotes()
        );
    }

    private UserEntity saveUser(String email) {
        UserEntity user = new UserEntity(
                email,
                "encoded-password",
                "Asset",
                "Owner"
        );

        return userRepository.saveAndFlush(user);
    }

    private PortfolioEntity savePortfolio(
            UserEntity user,
            String name
    ) {
        PortfolioEntity portfolio = new PortfolioEntity(
                user,
                name,
                PortfolioCreationMethod.BY_AMOUNT,
                new BigDecimal("10000.0000")
        );

        return portfolioRepository.saveAndFlush(portfolio);
    }

    private AssetEntity saveAsset(
            PortfolioEntity portfolio,
            String symbol,
            String displayName,
            AssetType assetType,
            String currency,
            String isin,
            String exchange,
            String notes
    ) {
        AssetEntity asset = new AssetEntity(
                portfolio,
                symbol,
                displayName,
                assetType,
                currency,
                isin,
                exchange,
                notes
        );

        return assetRepository.saveAndFlush(asset);
    }
}