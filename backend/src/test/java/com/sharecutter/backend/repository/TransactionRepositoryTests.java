package com.sharecutter.backend.repository;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.AssetType;
import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
import com.sharecutter.backend.domain.enums.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class TransactionRepositoryTests {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindActiveTransactionById() {
        UserEntity user = saveUser(
                "transaction.owner@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Primary Portfolio"
        );

        AssetEntity asset = saveAsset(
                portfolio,
                "AAPL",
                "Apple Inc."
        );

        OffsetDateTime executedAt =
                OffsetDateTime.of(
                        2026,
                        7,
                        1,
                        10,
                        30,
                        0,
                        0,
                        ZoneOffset.UTC
                );

        TransactionEntity transaction = saveTransaction(
                portfolio,
                asset,
                TransactionType.BUY,
                new BigDecimal("10.00000000"),
                new BigDecimal("200.00000000"),
                new BigDecimal("5.00000000"),
                new BigDecimal("2005.00000000"),
                "USD",
                executedAt,
                "Initial purchase"
        );

        Optional<TransactionEntity> result =
                transactionRepository
                        .findByIdAndDeletedAtIsNull(
                                transaction.getId()
                        );

        assertTrue(result.isPresent());

        TransactionEntity foundTransaction =
                result.get();

        assertEquals(
                transaction.getId(),
                foundTransaction.getId()
        );

        assertEquals(
                portfolio.getId(),
                foundTransaction.getPortfolio().getId()
        );

        assertEquals(
                asset.getId(),
                foundTransaction.getAsset().getId()
        );

        assertEquals(
                TransactionType.BUY,
                foundTransaction.getTransactionType()
        );

        assertEquals(
                0,
                new BigDecimal("10.00000000")
                        .compareTo(
                                foundTransaction.getQuantity()
                        )
        );

        assertEquals(
                0,
                new BigDecimal("200.00000000")
                        .compareTo(
                                foundTransaction.getUnitPrice()
                        )
        );

        assertEquals(
                0,
                new BigDecimal("5.00000000")
                        .compareTo(
                                foundTransaction.getFee()
                        )
        );

        assertEquals(
                0,
                new BigDecimal("2005.00000000")
                        .compareTo(
                                foundTransaction.getTotalAmount()
                        )
        );

        assertEquals(
                "USD",
                foundTransaction.getCurrency()
        );

        assertEquals(
                executedAt,
                foundTransaction.getExecutedAt()
        );

        assertEquals(
                "Initial purchase",
                foundTransaction.getNotes()
        );

        assertFalse(foundTransaction.isDeleted());
    }

    @Test
    void shouldFindTransactionOnlyInsideItsPortfolio() {
        UserEntity user = saveUser(
                "transaction.portfolio.owner@example.com"
        );

        PortfolioEntity firstPortfolio = savePortfolio(
                user,
                "First Portfolio"
        );

        PortfolioEntity secondPortfolio = savePortfolio(
                user,
                "Second Portfolio"
        );

        TransactionEntity transaction = saveTransaction(
                firstPortfolio,
                null,
                TransactionType.DEPOSIT,
                null,
                null,
                BigDecimal.ZERO,
                new BigDecimal("5000.00000000"),
                "USD",
                utcDateTime(
                        2026,
                        7,
                        2,
                        9,
                        0
                ),
                null
        );

        Optional<TransactionEntity> firstPortfolioResult =
                transactionRepository
                        .findByIdAndPortfolioIdAndDeletedAtIsNull(
                                transaction.getId(),
                                firstPortfolio.getId()
                        );

        Optional<TransactionEntity> secondPortfolioResult =
                transactionRepository
                        .findByIdAndPortfolioIdAndDeletedAtIsNull(
                                transaction.getId(),
                                secondPortfolio.getId()
                        );

        assertTrue(firstPortfolioResult.isPresent());
        assertTrue(secondPortfolioResult.isEmpty());
    }

    @Test
    void shouldReturnOnlyActiveTransactionsForPortfolioOrderedByExecutionTimeDescending() {
        UserEntity user = saveUser(
                "transaction.list.owner@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Transaction History Portfolio"
        );

        TransactionEntity oldestTransaction =
                saveTransaction(
                        portfolio,
                        null,
                        TransactionType.DEPOSIT,
                        null,
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("10000.00000000"),
                        "USD",
                        utcDateTime(
                                2026,
                                1,
                                1,
                                8,
                                0
                        ),
                        "Initial funding"
                );

        TransactionEntity newestTransaction =
                saveTransaction(
                        portfolio,
                        null,
                        TransactionType.WITHDRAWAL,
                        null,
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("1000.00000000"),
                        "USD",
                        utcDateTime(
                                2026,
                                3,
                                1,
                                8,
                                0
                        ),
                        "Partial withdrawal"
                );

        TransactionEntity middleTransaction =
                saveTransaction(
                        portfolio,
                        null,
                        TransactionType.FEE,
                        null,
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("25.00000000"),
                        "USD",
                        utcDateTime(
                                2026,
                                2,
                                1,
                                8,
                                0
                        ),
                        "Management fee"
                );

        TransactionEntity deletedTransaction =
                saveTransaction(
                        portfolio,
                        null,
                        TransactionType.DEPOSIT,
                        null,
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("500.00000000"),
                        "USD",
                        utcDateTime(
                                2026,
                                4,
                                1,
                                8,
                                0
                        ),
                        "Deleted funding"
                );

        deletedTransaction.softDelete();

        transactionRepository.saveAndFlush(
                deletedTransaction
        );

        List<TransactionEntity> result =
                transactionRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                                portfolio.getId()
                        );

        assertEquals(3, result.size());

        assertEquals(
                newestTransaction.getId(),
                result.get(0).getId()
        );

        assertEquals(
                middleTransaction.getId(),
                result.get(1).getId()
        );

        assertEquals(
                oldestTransaction.getId(),
                result.get(2).getId()
        );

        assertTrue(
                result.stream()
                        .noneMatch(
                                TransactionEntity::isDeleted
                        )
        );
    }

    @Test
    void shouldReturnOnlyActiveTransactionsForAsset() {
        UserEntity user = saveUser(
                "transaction.asset.owner@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Asset Transaction Portfolio"
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

        TransactionEntity appleBuy =
                saveTransaction(
                        portfolio,
                        appleAsset,
                        TransactionType.BUY,
                        new BigDecimal("5.00000000"),
                        new BigDecimal("180.00000000"),
                        new BigDecimal("2.00000000"),
                        new BigDecimal("902.00000000"),
                        "USD",
                        utcDateTime(
                                2026,
                                5,
                                1,
                                10,
                                0
                        ),
                        null
                );

        TransactionEntity appleSell =
                saveTransaction(
                        portfolio,
                        appleAsset,
                        TransactionType.SELL,
                        new BigDecimal("2.00000000"),
                        new BigDecimal("195.00000000"),
                        new BigDecimal("2.00000000"),
                        new BigDecimal("388.00000000"),
                        "USD",
                        utcDateTime(
                                2026,
                                6,
                                1,
                                10,
                                0
                        ),
                        null
                );

        saveTransaction(
                portfolio,
                microsoftAsset,
                TransactionType.BUY,
                new BigDecimal("3.00000000"),
                new BigDecimal("420.00000000"),
                new BigDecimal("2.00000000"),
                new BigDecimal("1262.00000000"),
                "USD",
                utcDateTime(
                        2026,
                        7,
                        1,
                        10,
                        0
                ),
                null
        );

        List<TransactionEntity> result =
                transactionRepository
                        .findAllByAssetIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                                appleAsset.getId()
                        );

        assertEquals(2, result.size());

        assertEquals(
                appleSell.getId(),
                result.get(0).getId()
        );

        assertEquals(
                appleBuy.getId(),
                result.get(1).getId()
        );

        assertTrue(
                result.stream()
                        .allMatch(
                                transaction ->
                                        transaction.getAsset()
                                                .getId()
                                                .equals(
                                                        appleAsset.getId()
                                                )
                        )
        );
    }

    @Test
    void shouldFilterTransactionsByType() {
        UserEntity user = saveUser(
                "transaction.type.owner@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Type Filter Portfolio"
        );

        AssetEntity asset = saveAsset(
                portfolio,
                "NVDA",
                "NVIDIA Corporation"
        );

        TransactionEntity firstBuy =
                saveTransaction(
                        portfolio,
                        asset,
                        TransactionType.BUY,
                        new BigDecimal("2.00000000"),
                        new BigDecimal("120.00000000"),
                        BigDecimal.ZERO,
                        new BigDecimal("240.00000000"),
                        "USD",
                        utcDateTime(
                                2026,
                                1,
                                10,
                                10,
                                0
                        ),
                        null
                );

        TransactionEntity secondBuy =
                saveTransaction(
                        portfolio,
                        asset,
                        TransactionType.BUY,
                        new BigDecimal("3.00000000"),
                        new BigDecimal("130.00000000"),
                        BigDecimal.ZERO,
                        new BigDecimal("390.00000000"),
                        "USD",
                        utcDateTime(
                                2026,
                                2,
                                10,
                                10,
                                0
                        ),
                        null
                );

        saveTransaction(
                portfolio,
                asset,
                TransactionType.SELL,
                new BigDecimal("1.00000000"),
                new BigDecimal("150.00000000"),
                BigDecimal.ZERO,
                new BigDecimal("150.00000000"),
                "USD",
                utcDateTime(
                        2026,
                        3,
                        10,
                        10,
                        0
                ),
                null
        );

        List<TransactionEntity> result =
                transactionRepository
                        .findAllByPortfolioIdAndTransactionTypeAndDeletedAtIsNullOrderByExecutedAtDesc(
                                portfolio.getId(),
                                TransactionType.BUY
                        );

        assertEquals(2, result.size());

        assertEquals(
                secondBuy.getId(),
                result.get(0).getId()
        );

        assertEquals(
                firstBuy.getId(),
                result.get(1).getId()
        );

        assertTrue(
                result.stream()
                        .allMatch(
                                transaction ->
                                        transaction.getTransactionType()
                                                == TransactionType.BUY
                        )
        );
    }

    @Test
    void shouldReturnTransactionsInsideExecutionDateRange() {
        UserEntity user = saveUser(
                "transaction.range.owner@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Date Range Portfolio"
        );

        saveTransaction(
                portfolio,
                null,
                TransactionType.DEPOSIT,
                null,
                null,
                BigDecimal.ZERO,
                new BigDecimal("1000.00000000"),
                "USD",
                utcDateTime(
                        2026,
                        1,
                        1,
                        0,
                        0
                ),
                null
        );

        TransactionEntity februaryTransaction =
                saveTransaction(
                        portfolio,
                        null,
                        TransactionType.DEPOSIT,
                        null,
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("2000.00000000"),
                        "USD",
                        utcDateTime(
                                2026,
                                2,
                                1,
                                0,
                                0
                        ),
                        null
                );

        TransactionEntity marchTransaction =
                saveTransaction(
                        portfolio,
                        null,
                        TransactionType.WITHDRAWAL,
                        null,
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("500.00000000"),
                        "USD",
                        utcDateTime(
                                2026,
                                3,
                                1,
                                0,
                                0
                        ),
                        null
                );

        saveTransaction(
                portfolio,
                null,
                TransactionType.FEE,
                null,
                null,
                BigDecimal.ZERO,
                new BigDecimal("20.00000000"),
                "USD",
                utcDateTime(
                        2026,
                        4,
                        1,
                        0,
                        0
                ),
                null
        );

        OffsetDateTime startDate =
                utcDateTime(
                        2026,
                        2,
                        1,
                        0,
                        0
                );

        OffsetDateTime endDate =
                utcDateTime(
                        2026,
                        3,
                        31,
                        23,
                        59
                );

        List<TransactionEntity> result =
                transactionRepository
                        .findAllByPortfolioIdAndExecutedAtBetweenAndDeletedAtIsNullOrderByExecutedAtDesc(
                                portfolio.getId(),
                                startDate,
                                endDate
                        );

        assertEquals(2, result.size());

        assertEquals(
                marchTransaction.getId(),
                result.get(0).getId()
        );

        assertEquals(
                februaryTransaction.getId(),
                result.get(1).getId()
        );
    }

    @Test
    void shouldNotReturnSoftDeletedTransaction() {
        UserEntity user = saveUser(
                "transaction.deleted.owner@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Deleted Transaction Portfolio"
        );

        TransactionEntity transaction =
                saveTransaction(
                        portfolio,
                        null,
                        TransactionType.DEPOSIT,
                        null,
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("3000.00000000"),
                        "USD",
                        utcDateTime(
                                2026,
                                6,
                                1,
                                12,
                                0
                        ),
                        null
                );

        transaction.softDelete();

        transactionRepository.saveAndFlush(
                transaction
        );

        Optional<TransactionEntity> resultById =
                transactionRepository
                        .findByIdAndDeletedAtIsNull(
                                transaction.getId()
                        );

        Optional<TransactionEntity> resultByPortfolio =
                transactionRepository
                        .findByIdAndPortfolioIdAndDeletedAtIsNull(
                                transaction.getId(),
                                portfolio.getId()
                        );

        boolean exists =
                transactionRepository
                        .existsByIdAndPortfolioIdAndDeletedAtIsNull(
                                transaction.getId(),
                                portfolio.getId()
                        );

        assertTrue(resultById.isEmpty());
        assertTrue(resultByPortfolio.isEmpty());
        assertFalse(exists);
    }

    @Test
    void shouldDetectTransactionOnlyInsideCorrectPortfolio() {
        UserEntity user = saveUser(
                "transaction.exists.owner@example.com"
        );

        PortfolioEntity firstPortfolio = savePortfolio(
                user,
                "Owned Portfolio"
        );

        PortfolioEntity secondPortfolio = savePortfolio(
                user,
                "Other Portfolio"
        );

        TransactionEntity transaction =
                saveTransaction(
                        firstPortfolio,
                        null,
                        TransactionType.DEPOSIT,
                        null,
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("7500.00000000"),
                        "USD",
                        utcDateTime(
                                2026,
                                7,
                                1,
                                8,
                                0
                        ),
                        null
                );

        boolean correctPortfolioResult =
                transactionRepository
                        .existsByIdAndPortfolioIdAndDeletedAtIsNull(
                                transaction.getId(),
                                firstPortfolio.getId()
                        );

        boolean wrongPortfolioResult =
                transactionRepository
                        .existsByIdAndPortfolioIdAndDeletedAtIsNull(
                                transaction.getId(),
                                secondPortfolio.getId()
                        );

        assertTrue(correctPortfolioResult);
        assertFalse(wrongPortfolioResult);
    }

    @Test
    void shouldNormalizeTransactionValuesBeforeSaving() {
        UserEntity user = saveUser(
                "transaction.normalize.owner@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Normalization Portfolio"
        );

        TransactionEntity transaction =
                saveTransaction(
                        portfolio,
                        null,
                        TransactionType.DEPOSIT,
                        null,
                        null,
                        null,
                        new BigDecimal("12000.00000000"),
                        "  usd  ",
                        utcDateTime(
                                2026,
                                7,
                                15,
                                9,
                                30
                        ),
                        "  Initial account funding  "
                );

        assertEquals(
                "USD",
                transaction.getCurrency()
        );

        assertEquals(
                "Initial account funding",
                transaction.getNotes()
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        transaction.getFee()
                )
        );
    }

    private UserEntity saveUser(
            String email
    ) {
        UserEntity user = new UserEntity(
                email,
                "encoded-password",
                "Transaction",
                "Owner"
        );

        return userRepository.saveAndFlush(user);
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
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal fee,
            BigDecimal totalAmount,
            String currency,
            OffsetDateTime executedAt,
            String notes
    ) {
        TransactionEntity transaction =
                new TransactionEntity(
                        portfolio,
                        asset,
                        transactionType,
                        quantity,
                        unitPrice,
                        fee,
                        totalAmount,
                        currency,
                        executedAt,
                        notes
                );

        return transactionRepository.saveAndFlush(
                transaction
        );
    }

    private OffsetDateTime utcDateTime(
            int year,
            int month,
            int day,
            int hour,
            int minute
    ) {
        return OffsetDateTime.of(
                year,
                month,
                day,
                hour,
                minute,
                0,
                0,
                ZoneOffset.UTC
        );
    }
}