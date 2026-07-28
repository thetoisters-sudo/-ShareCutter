package com.sharecutter.backend.repository;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.AssetType;
import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
import com.sharecutter.backend.domain.enums.TransactionType;
import com.sharecutter.backend.repository.projection.AssetAllocationProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class TransactionAllocationRepositoryTests {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldAggregateBuyAndSellTransactionsByAsset() {
        UserEntity user = saveUser(
                "allocation.aggregate@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Allocation Portfolio"
        );

        AssetEntity appleAsset = saveAsset(
                portfolio,
                "AAPL",
                "Apple Inc."
        );

        saveTransaction(
                portfolio,
                appleAsset,
                TransactionType.BUY,
                "10.00000000",
                "180.00000000",
                "5.00000000",
                "1805.00000000"
        );

        saveTransaction(
                portfolio,
                appleAsset,
                TransactionType.BUY,
                "5.00000000",
                "200.00000000",
                "3.00000000",
                "1003.00000000"
        );

        saveTransaction(
                portfolio,
                appleAsset,
                TransactionType.SELL,
                "4.00000000",
                "220.00000000",
                "2.00000000",
                "878.00000000"
        );

        List<AssetAllocationProjection> result =
                transactionRepository
                        .findAssetAllocationByPortfolioId(
                                portfolio.getId()
                        );

        assertEquals(1, result.size());

        AssetAllocationProjection allocation =
                result.getFirst();

        assertEquals(
                appleAsset.getId(),
                allocation.getAssetId()
        );

        assertEquals(
                "AAPL",
                allocation.getSymbol()
        );

        assertEquals(
                "Apple Inc.",
                allocation.getDisplayName()
        );

        assertEquals(
                AssetType.STOCK,
                allocation.getAssetType()
        );

        assertEquals(
                "USD",
                allocation.getCurrency()
        );

        assertBigDecimalEquals(
                "15.00000000",
                allocation.getBoughtQuantity()
        );

        assertBigDecimalEquals(
                "4.00000000",
                allocation.getSoldQuantity()
        );

        assertBigDecimalEquals(
                "2808.00000000",
                allocation.getTotalBuyAmount()
        );

        assertBigDecimalEquals(
                "878.00000000",
                allocation.getTotalSellAmount()
        );
    }

    @Test
    void shouldSeparateAllocationBetweenAssets() {
        UserEntity user = saveUser(
                "allocation.assets@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Multi Asset Portfolio"
        );

        AssetEntity appleAsset = saveAsset(
                portfolio,
                "AAPL",
                "Apple Inc."
        );

        AssetEntity microsoftAsset = saveAsset(
                portfolio,
                "MSFT",
                "Microsoft Corporation"
        );

        saveTransaction(
                portfolio,
                appleAsset,
                TransactionType.BUY,
                "10.00000000",
                "180.00000000",
                "0.00000000",
                "1800.00000000"
        );

        saveTransaction(
                portfolio,
                microsoftAsset,
                TransactionType.BUY,
                "5.00000000",
                "400.00000000",
                "0.00000000",
                "2000.00000000"
        );

        List<AssetAllocationProjection> result =
                transactionRepository
                        .findAssetAllocationByPortfolioId(
                                portfolio.getId()
                        );

        assertEquals(2, result.size());

        AssetAllocationProjection appleAllocation =
                findBySymbol(
                        result,
                        "AAPL"
                );

        AssetAllocationProjection microsoftAllocation =
                findBySymbol(
                        result,
                        "MSFT"
                );

        assertBigDecimalEquals(
                "10.00000000",
                appleAllocation.getBoughtQuantity()
        );

        assertBigDecimalEquals(
                "1800.00000000",
                appleAllocation.getTotalBuyAmount()
        );

        assertBigDecimalEquals(
                "5.00000000",
                microsoftAllocation.getBoughtQuantity()
        );

        assertBigDecimalEquals(
                "2000.00000000",
                microsoftAllocation.getTotalBuyAmount()
        );
    }

    @Test
    void shouldIgnoreNonTradingAndSoftDeletedTransactions() {
        UserEntity user = saveUser(
                "allocation.filters@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Filtered Allocation Portfolio"
        );

        AssetEntity asset = saveAsset(
                portfolio,
                "NVDA",
                "NVIDIA Corporation"
        );

        saveTransaction(
                portfolio,
                asset,
                TransactionType.BUY,
                "4.00000000",
                "150.00000000",
                "0.00000000",
                "600.00000000"
        );

        TransactionEntity deletedBuy =
                saveTransaction(
                        portfolio,
                        asset,
                        TransactionType.BUY,
                        "6.00000000",
                        "150.00000000",
                        "0.00000000",
                        "900.00000000"
                );

        deletedBuy.softDelete();

        transactionRepository.saveAndFlush(
                deletedBuy
        );

        saveTransaction(
                portfolio,
                asset,
                TransactionType.DIVIDEND,
                null,
                null,
                "0.00000000",
                "50.00000000"
        );

        saveTransaction(
                portfolio,
                asset,
                TransactionType.FEE,
                null,
                null,
                "0.00000000",
                "10.00000000"
        );

        List<AssetAllocationProjection> result =
                transactionRepository
                        .findAssetAllocationByPortfolioId(
                                portfolio.getId()
                        );

        assertEquals(1, result.size());

        AssetAllocationProjection allocation =
                result.getFirst();

        assertBigDecimalEquals(
                "4.00000000",
                allocation.getBoughtQuantity()
        );

        assertBigDecimalEquals(
                "0",
                allocation.getSoldQuantity()
        );

        assertBigDecimalEquals(
                "600.00000000",
                allocation.getTotalBuyAmount()
        );

        assertBigDecimalEquals(
                "0",
                allocation.getTotalSellAmount()
        );
    }

    @Test
    void shouldIgnoreSoftDeletedAssets() {
        UserEntity user = saveUser(
                "allocation.deleted.asset@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Deleted Asset Portfolio"
        );

        AssetEntity activeAsset = saveAsset(
                portfolio,
                "AAPL",
                "Apple Inc."
        );

        AssetEntity deletedAsset = saveAsset(
                portfolio,
                "TSLA",
                "Tesla Inc."
        );

        saveTransaction(
                portfolio,
                activeAsset,
                TransactionType.BUY,
                "2.00000000",
                "200.00000000",
                "0.00000000",
                "400.00000000"
        );

        saveTransaction(
                portfolio,
                deletedAsset,
                TransactionType.BUY,
                "3.00000000",
                "300.00000000",
                "0.00000000",
                "900.00000000"
        );

        deletedAsset.softDelete();

        assetRepository.saveAndFlush(
                deletedAsset
        );

        List<AssetAllocationProjection> result =
                transactionRepository
                        .findAssetAllocationByPortfolioId(
                                portfolio.getId()
                        );

        assertEquals(1, result.size());

        assertEquals(
                activeAsset.getId(),
                result.getFirst().getAssetId()
        );

        assertEquals(
                "AAPL",
                result.getFirst().getSymbol()
        );
    }

    @Test
    void shouldReturnOnlyAllocationForRequestedPortfolio() {
        UserEntity user = saveUser(
                "allocation.portfolios@example.com"
        );

        PortfolioEntity firstPortfolio = savePortfolio(
                user,
                "First Allocation Portfolio"
        );

        PortfolioEntity secondPortfolio = savePortfolio(
                user,
                "Second Allocation Portfolio"
        );

        AssetEntity firstAsset = saveAsset(
                firstPortfolio,
                "AAPL",
                "Apple Inc."
        );

        AssetEntity secondAsset = saveAsset(
                secondPortfolio,
                "MSFT",
                "Microsoft Corporation"
        );

        saveTransaction(
                firstPortfolio,
                firstAsset,
                TransactionType.BUY,
                "10.00000000",
                "100.00000000",
                "0.00000000",
                "1000.00000000"
        );

        saveTransaction(
                secondPortfolio,
                secondAsset,
                TransactionType.BUY,
                "20.00000000",
                "100.00000000",
                "0.00000000",
                "2000.00000000"
        );

        List<AssetAllocationProjection> result =
                transactionRepository
                        .findAssetAllocationByPortfolioId(
                                firstPortfolio.getId()
                        );

        assertEquals(1, result.size());

        assertEquals(
                firstAsset.getId(),
                result.getFirst().getAssetId()
        );

        assertEquals(
                "AAPL",
                result.getFirst().getSymbol()
        );

        assertBigDecimalEquals(
                "1000.00000000",
                result.getFirst()
                        .getTotalBuyAmount()
        );
    }

    @Test
    void shouldReturnEmptyAllocationWhenPortfolioHasNoTradingTransactions() {
        UserEntity user = saveUser(
                "allocation.empty@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Empty Allocation Portfolio"
        );

        saveTransaction(
                portfolio,
                null,
                TransactionType.DEPOSIT,
                null,
                null,
                "0.00000000",
                "10000.00000000"
        );

        List<AssetAllocationProjection> result =
                transactionRepository
                        .findAssetAllocationByPortfolioId(
                                portfolio.getId()
                        );

        assertTrue(result.isEmpty());
    }

    private UserEntity saveUser(
            String email
    ) {
        UserEntity user = new UserEntity(
                email,
                "encoded-password",
                "Allocation",
                "Owner"
        );

        return userRepository.saveAndFlush(
                user
        );
    }

    private PortfolioEntity savePortfolio(
            UserEntity user,
            String name
    ) {
        PortfolioEntity portfolio =
                new PortfolioEntity(
                        user,
                        name,
                        PortfolioCreationMethod.BY_AMOUNT,
                        new BigDecimal("10000.0000")
                );

        return portfolioRepository.saveAndFlush(
                portfolio
        );
    }

    private AssetEntity saveAsset(
            PortfolioEntity portfolio,
            String symbol,
            String displayName
    ) {
        AssetEntity asset = new AssetEntity(
                portfolio,
                symbol,
                displayName,
                AssetType.STOCK,
                "USD",
                null,
                "NASDAQ",
                null
        );

        return assetRepository.saveAndFlush(
                asset
        );
    }

    private TransactionEntity saveTransaction(
            PortfolioEntity portfolio,
            AssetEntity asset,
            TransactionType transactionType,
            String quantity,
            String unitPrice,
            String fee,
            String totalAmount
    ) {
        TransactionEntity transaction =
                new TransactionEntity(
                        portfolio,
                        asset,
                        transactionType,
                        toBigDecimal(quantity),
                        toBigDecimal(unitPrice),
                        toBigDecimal(fee),
                        new BigDecimal(totalAmount),
                        "USD",
                        OffsetDateTime.of(
                                2026,
                                7,
                                28,
                                8,
                                0,
                                0,
                                0,
                                ZoneOffset.UTC
                        ),
                        null
                );

        return transactionRepository.saveAndFlush(
                transaction
        );
    }

    private AssetAllocationProjection findBySymbol(
            List<AssetAllocationProjection> allocations,
            String symbol
    ) {
        return allocations
                .stream()
                .filter(
                        allocation ->
                                allocation.getSymbol()
                                        .equals(symbol)
                )
                .findFirst()
                .orElseThrow(
                        () -> new AssertionError(
                                "Allocation was not found "
                                        + "for symbol "
                                        + symbol
                        )
                );
    }

    private BigDecimal toBigDecimal(
            String value
    ) {
        return value == null
                ? null
                : new BigDecimal(value);
    }

    private void assertBigDecimalEquals(
            String expected,
            BigDecimal actual
    ) {
        assertEquals(
                0,
                new BigDecimal(expected)
                        .compareTo(actual)
        );
    }
}
