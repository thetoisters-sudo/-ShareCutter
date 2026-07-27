package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.AssetType;
import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
import com.sharecutter.backend.domain.enums.TransactionType;
import com.sharecutter.backend.exception.InvalidTransactionException;
import com.sharecutter.backend.exception.TransactionNotFoundException;
import com.sharecutter.backend.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTests {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private AssetService assetService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository,
                portfolioService,
                assetService
        );
    }

    @Test
    void shouldCreateValidBuyTransaction() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.buy@example.com",
                "Buy Portfolio"
        );

        AssetEntity asset = createAsset(
                portfolio,
                "AAPL",
                "Apple Inc."
        );

        OffsetDateTime executedAt =
                pastExecutionTime();

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetService.getAsset(
                userId,
                portfolioId,
                assetId
        )).thenReturn(asset);

        when(transactionRepository.save(
                any(TransactionEntity.class)
        )).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        TransactionEntity result =
                transactionService.createTransaction(
                        userId,
                        portfolioId,
                        assetId,
                        TransactionType.BUY,
                        new BigDecimal("10.00000000"),
                        new BigDecimal("150.25000000"),
                        new BigDecimal("5.00000000"),
                        new BigDecimal("1507.50000000"),
                        "usd",
                        executedAt,
                        "Initial purchase"
                );

        ArgumentCaptor<TransactionEntity> captor =
                ArgumentCaptor.forClass(
                        TransactionEntity.class
                );

        verify(transactionRepository)
                .save(captor.capture());

        TransactionEntity savedTransaction =
                captor.getValue();

        assertThat(result)
                .isSameAs(savedTransaction);

        assertThat(savedTransaction.getPortfolio())
                .isSameAs(portfolio);

        assertThat(savedTransaction.getAsset())
                .isSameAs(asset);

        assertThat(savedTransaction.getTransactionType())
                .isEqualTo(TransactionType.BUY);

        assertThat(savedTransaction.getQuantity())
                .isEqualByComparingTo("10.00000000");

        assertThat(savedTransaction.getUnitPrice())
                .isEqualByComparingTo("150.25000000");

        assertThat(savedTransaction.getFee())
                .isEqualByComparingTo("5.00000000");

        assertThat(savedTransaction.getTotalAmount())
                .isEqualByComparingTo("1507.50000000");

        assertThat(savedTransaction.getCurrency())
                .isEqualTo("USD");

        assertThat(savedTransaction.getExecutedAt())
                .isEqualTo(executedAt);

        assertThat(savedTransaction.getNotes())
                .isEqualTo("Initial purchase");

        assertThat(savedTransaction.isDeleted())
                .isFalse();

        verify(portfolioService).getPortfolio(
                userId,
                portfolioId
        );

        verify(assetService).getAsset(
                userId,
                portfolioId,
                assetId
        );
    }

    @Test
    void shouldCreateDepositWithoutAsset() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.deposit@example.com",
                "Deposit Portfolio"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(transactionRepository.save(
                any(TransactionEntity.class)
        )).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        TransactionEntity result =
                transactionService.createTransaction(
                        userId,
                        portfolioId,
                        null,
                        TransactionType.DEPOSIT,
                        null,
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("5000.00000000"),
                        "ILS",
                        pastExecutionTime(),
                        "Account deposit"
                );

        assertThat(result.getPortfolio())
                .isSameAs(portfolio);

        assertThat(result.getAsset())
                .isNull();

        assertThat(result.getTransactionType())
                .isEqualTo(TransactionType.DEPOSIT);

        assertThat(result.getQuantity())
                .isNull();

        assertThat(result.getUnitPrice())
                .isNull();

        assertThat(result.getTotalAmount())
                .isEqualByComparingTo("5000.00000000");

        verify(assetService, never()).getAsset(
                any(UUID.class),
                any(UUID.class),
                any(UUID.class)
        );

        verify(transactionRepository)
                .save(any(TransactionEntity.class));
    }

    @Test
    void shouldRejectBuyWithoutAsset() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.buy.no.asset@example.com",
                "Missing Asset Portfolio"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        assertThatThrownBy(
                () -> transactionService.createTransaction(
                        userId,
                        portfolioId,
                        null,
                        TransactionType.BUY,
                        new BigDecimal("2.00000000"),
                        new BigDecimal("100.00000000"),
                        BigDecimal.ZERO,
                        new BigDecimal("200.00000000"),
                        "USD",
                        pastExecutionTime(),
                        null
                )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "BUY transaction must reference an asset"
                );

        verify(assetService, never()).getAsset(
                any(UUID.class),
                any(UUID.class),
                any(UUID.class)
        );

        verify(transactionRepository, never())
                .save(any(TransactionEntity.class));
    }

    @Test
    void shouldRejectSellWithoutQuantity() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.sell.no.quantity@example.com",
                "Missing Quantity Portfolio"
        );

        AssetEntity asset = createAsset(
                portfolio,
                "MSFT",
                "Microsoft Corporation"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetService.getAsset(
                userId,
                portfolioId,
                assetId
        )).thenReturn(asset);

        assertThatThrownBy(
                () -> transactionService.createTransaction(
                        userId,
                        portfolioId,
                        assetId,
                        TransactionType.SELL,
                        null,
                        new BigDecimal("250.00000000"),
                        BigDecimal.ZERO,
                        new BigDecimal("500.00000000"),
                        "USD",
                        pastExecutionTime(),
                        null
                )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "SELL transaction must include a quantity"
                );

        verify(transactionRepository, never())
                .save(any(TransactionEntity.class));
    }

    @Test
    void shouldRejectBuyWithoutUnitPrice() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.buy.no.price@example.com",
                "Missing Price Portfolio"
        );

        AssetEntity asset = createAsset(
                portfolio,
                "NVDA",
                "NVIDIA Corporation"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetService.getAsset(
                userId,
                portfolioId,
                assetId
        )).thenReturn(asset);

        assertThatThrownBy(
                () -> transactionService.createTransaction(
                        userId,
                        portfolioId,
                        assetId,
                        TransactionType.BUY,
                        new BigDecimal("3.00000000"),
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("300.00000000"),
                        "USD",
                        pastExecutionTime(),
                        null
                )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "BUY transaction must include a unit price"
                );

        verify(transactionRepository, never())
                .save(any(TransactionEntity.class));
    }

    @Test
    void shouldRejectDividendWithoutAsset() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.dividend@example.com",
                "Dividend Portfolio"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        assertThatThrownBy(
                () -> transactionService.createTransaction(
                        userId,
                        portfolioId,
                        null,
                        TransactionType.DIVIDEND,
                        null,
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("25.00000000"),
                        "USD",
                        pastExecutionTime(),
                        null
                )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "DIVIDEND transaction must reference an asset"
                );

        verify(transactionRepository, never())
                .save(any(TransactionEntity.class));
    }

    @Test
    void shouldRejectDepositWithAsset() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.deposit.asset@example.com",
                "Invalid Deposit Portfolio"
        );

        AssetEntity asset = createAsset(
                portfolio,
                "BTC",
                "Bitcoin"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetService.getAsset(
                userId,
                portfolioId,
                assetId
        )).thenReturn(asset);

        assertThatThrownBy(
                () -> transactionService.createTransaction(
                        userId,
                        portfolioId,
                        assetId,
                        TransactionType.DEPOSIT,
                        null,
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("1000.00000000"),
                        "USD",
                        pastExecutionTime(),
                        null
                )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "DEPOSIT transaction must not reference an asset"
                );

        verify(transactionRepository, never())
                .save(any(TransactionEntity.class));
    }

    @Test
    void shouldRejectFeeGreaterThanTotalAmount() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.fee@example.com",
                "Fee Portfolio"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        assertThatThrownBy(
                () -> transactionService.createTransaction(
                        userId,
                        portfolioId,
                        null,
                        TransactionType.FEE,
                        null,
                        null,
                        new BigDecimal("101.00000000"),
                        new BigDecimal("100.00000000"),
                        "ILS",
                        pastExecutionTime(),
                        null
                )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "Transaction fee must not exceed total amount"
                );

        verify(transactionRepository, never())
                .save(any(TransactionEntity.class));
    }

    @Test
    void shouldRejectFutureExecutionTime() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.future@example.com",
                "Future Portfolio"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        OffsetDateTime futureExecutionTime =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                ).plusDays(1);

        assertThatThrownBy(
                () -> transactionService.createTransaction(
                        userId,
                        portfolioId,
                        null,
                        TransactionType.WITHDRAWAL,
                        null,
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("250.00000000"),
                        "ILS",
                        futureExecutionTime,
                        null
                )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "Transaction execution time must not be in the future"
                );

        verify(transactionRepository, never())
                .save(any(TransactionEntity.class));
    }

    @Test
    void shouldReturnOwnedActiveTransaction() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.get@example.com",
                "Get Portfolio"
        );

        TransactionEntity transaction =
                createCashTransaction(
                        portfolio,
                        TransactionType.DEPOSIT,
                        "1000.00000000"
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(transactionRepository
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        transactionId,
                        portfolioId
                ))
                .thenReturn(Optional.of(transaction));

        TransactionEntity result =
                transactionService.getTransaction(
                        userId,
                        portfolioId,
                        transactionId
                );

        assertThat(result)
                .isSameAs(transaction);

        verify(portfolioService).getPortfolio(
                userId,
                portfolioId
        );

        verify(transactionRepository)
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        transactionId,
                        portfolioId
                );
    }

    @Test
    void shouldThrowWhenTransactionDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.missing@example.com",
                "Missing Transaction Portfolio"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(transactionRepository
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        transactionId,
                        portfolioId
                ))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> transactionService.getTransaction(
                        userId,
                        portfolioId,
                        transactionId
                )
        )
                .isInstanceOf(
                        TransactionNotFoundException.class
                )
                .hasMessageContaining(
                        transactionId.toString()
                );
    }

    @Test
    void shouldReturnAllActivePortfolioTransactions() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.list@example.com",
                "Transaction List Portfolio"
        );

        TransactionEntity firstTransaction =
                createCashTransaction(
                        portfolio,
                        TransactionType.DEPOSIT,
                        "2000.00000000"
                );

        TransactionEntity secondTransaction =
                createCashTransaction(
                        portfolio,
                        TransactionType.WITHDRAWAL,
                        "500.00000000"
                );

        List<TransactionEntity> transactions =
                List.of(
                        firstTransaction,
                        secondTransaction
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(transactionRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                ))
                .thenReturn(transactions);

        List<TransactionEntity> result =
                transactionService
                        .getPortfolioTransactions(
                                userId,
                                portfolioId
                        );

        assertThat(result)
                .containsExactly(
                        firstTransaction,
                        secondTransaction
                );

        verify(transactionRepository)
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                );
    }

    @Test
    void shouldReturnTransactionsForOwnedAsset() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        AssetEntity asset = mock(
                AssetEntity.class
        );

        when(asset.getId())
                .thenReturn(assetId);

        TransactionEntity transaction =
                mock(TransactionEntity.class);

        when(assetService.getAsset(
                userId,
                portfolioId,
                assetId
        )).thenReturn(asset);

        when(transactionRepository
                .findAllByAssetIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        assetId
                ))
                .thenReturn(List.of(transaction));

        List<TransactionEntity> result =
                transactionService.getAssetTransactions(
                        userId,
                        portfolioId,
                        assetId
                );

        assertThat(result)
                .containsExactly(transaction);

        verify(assetService).getAsset(
                userId,
                portfolioId,
                assetId
        );

        verify(transactionRepository)
                .findAllByAssetIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        assetId
                );
    }

    @Test
    void shouldReturnTransactionsByType() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.type@example.com",
                "Type Portfolio"
        );

        TransactionEntity transaction =
                createCashTransaction(
                        portfolio,
                        TransactionType.DEPOSIT,
                        "3000.00000000"
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(transactionRepository
                .findAllByPortfolioIdAndTransactionTypeAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId,
                        TransactionType.DEPOSIT
                ))
                .thenReturn(List.of(transaction));

        List<TransactionEntity> result =
                transactionService.getTransactionsByType(
                        userId,
                        portfolioId,
                        TransactionType.DEPOSIT
                );

        assertThat(result)
                .containsExactly(transaction);

        verify(transactionRepository)
                .findAllByPortfolioIdAndTransactionTypeAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId,
                        TransactionType.DEPOSIT
                );
    }

    @Test
    void shouldRejectNullTransactionTypeFilter() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.type.null@example.com",
                "Null Type Portfolio"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        assertThatThrownBy(
                () -> transactionService
                        .getTransactionsByType(
                                userId,
                                portfolioId,
                                null
                        )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "Transaction type must not be null"
                );
    }

    @Test
    void shouldReturnTransactionsByValidDateRange() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.range@example.com",
                "Date Range Portfolio"
        );

        OffsetDateTime startDate =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                ).minusDays(30);

        OffsetDateTime endDate =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                ).minusDays(1);

        TransactionEntity transaction =
                createCashTransaction(
                        portfolio,
                        TransactionType.DEPOSIT,
                        "1500.00000000"
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(transactionRepository
                .findAllByPortfolioIdAndExecutedAtBetweenAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId,
                        startDate,
                        endDate
                ))
                .thenReturn(List.of(transaction));

        List<TransactionEntity> result =
                transactionService
                        .getTransactionsByDateRange(
                                userId,
                                portfolioId,
                                startDate,
                                endDate
                        );

        assertThat(result)
                .containsExactly(transaction);

        verify(transactionRepository)
                .findAllByPortfolioIdAndExecutedAtBetweenAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId,
                        startDate,
                        endDate
                );
    }

    @Test
    void shouldRejectReversedDateRange() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.range.invalid@example.com",
                "Invalid Range Portfolio"
        );

        OffsetDateTime startDate =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                );

        OffsetDateTime endDate =
                startDate.minusDays(1);

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        assertThatThrownBy(
                () -> transactionService
                        .getTransactionsByDateRange(
                                userId,
                                portfolioId,
                                startDate,
                                endDate
                        )
        )
                .isInstanceOf(
                        InvalidTransactionException.class
                )
                .hasMessageContaining(
                        "Transaction start date must not be after end date"
                );

        verify(transactionRepository, never())
                .findAllByPortfolioIdAndExecutedAtBetweenAndDeletedAtIsNullOrderByExecutedAtDesc(
                        any(UUID.class),
                        any(OffsetDateTime.class),
                        any(OffsetDateTime.class)
                );
    }

    @Test
    void shouldUpdateOwnedActiveTransaction() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.update@example.com",
                "Update Portfolio"
        );

        AssetEntity asset = createAsset(
                portfolio,
                "TSLA",
                "Tesla Inc."
        );

        TransactionEntity transaction =
                createCashTransaction(
                        portfolio,
                        TransactionType.DEPOSIT,
                        "5000.00000000"
                );

        OffsetDateTime executedAt =
                pastExecutionTime();

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(transactionRepository
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        transactionId,
                        portfolioId
                ))
                .thenReturn(Optional.of(transaction));

        when(assetService.getAsset(
                userId,
                portfolioId,
                assetId
        )).thenReturn(asset);

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        TransactionEntity result =
                transactionService.updateTransaction(
                        userId,
                        portfolioId,
                        transactionId,
                        assetId,
                        TransactionType.BUY,
                        new BigDecimal("4.00000000"),
                        new BigDecimal("200.00000000"),
                        new BigDecimal("2.00000000"),
                        new BigDecimal("802.00000000"),
                        "usd",
                        executedAt,
                        "Converted to purchase"
                );

        assertThat(result)
                .isSameAs(transaction);

        assertThat(transaction.getAsset())
                .isSameAs(asset);

        assertThat(transaction.getTransactionType())
                .isEqualTo(TransactionType.BUY);

        assertThat(transaction.getQuantity())
                .isEqualByComparingTo("4.00000000");

        assertThat(transaction.getUnitPrice())
                .isEqualByComparingTo("200.00000000");

        assertThat(transaction.getFee())
                .isEqualByComparingTo("2.00000000");

        assertThat(transaction.getTotalAmount())
                .isEqualByComparingTo("802.00000000");

        assertThat(transaction.getCurrency())
                .isEqualTo("USD");

        assertThat(transaction.getExecutedAt())
                .isEqualTo(executedAt);

        assertThat(transaction.getNotes())
                .isEqualTo("Converted to purchase");

        verify(transactionRepository)
                .save(transaction);
    }

    @Test
    void shouldSoftDeleteOwnedActiveTransaction() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "transaction.delete@example.com",
                "Delete Portfolio"
        );

        TransactionEntity transaction =
                createCashTransaction(
                        portfolio,
                        TransactionType.DEPOSIT,
                        "1000.00000000"
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(transactionRepository
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        transactionId,
                        portfolioId
                ))
                .thenReturn(Optional.of(transaction));

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        assertThat(transaction.isDeleted())
                .isFalse();

        transactionService.deleteTransaction(
                userId,
                portfolioId,
                transactionId
        );

        assertThat(transaction.isDeleted())
                .isTrue();

        assertThat(transaction.getDeletedAt())
                .isNotNull();

        verify(transactionRepository)
                .save(transaction);

        verify(transactionRepository, never())
                .delete(transaction);
    }

    private UserEntity createUser(
            String email
    ) {
        return new UserEntity(
                email,
                "hashed-password",
                "Transaction",
                "User"
        );
    }

    private PortfolioEntity createPortfolio(
            String userEmail,
            String portfolioName
    ) {
        UserEntity user = createUser(
                userEmail
        );

        return new PortfolioEntity(
                user,
                portfolioName,
                PortfolioCreationMethod.BY_AMOUNT,
                new BigDecimal("10000.0000")
        );
    }

    private AssetEntity createAsset(
            PortfolioEntity portfolio,
            String symbol,
            String displayName
    ) {
        return new AssetEntity(
                portfolio,
                symbol,
                displayName,
                AssetType.STOCK,
                "USD",
                null,
                "NASDAQ",
                null
        );
    }

    private TransactionEntity createCashTransaction(
            PortfolioEntity portfolio,
            TransactionType transactionType,
            String totalAmount
    ) {
        return new TransactionEntity(
                portfolio,
                null,
                transactionType,
                null,
                null,
                BigDecimal.ZERO,
                new BigDecimal(totalAmount),
                "ILS",
                pastExecutionTime(),
                null
        );
    }

    private OffsetDateTime pastExecutionTime() {
        return OffsetDateTime.now(
                ZoneOffset.UTC
        ).minusHours(1);
    }
}