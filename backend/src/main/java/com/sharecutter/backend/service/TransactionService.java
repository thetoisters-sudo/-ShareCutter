package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.domain.enums.TransactionType;
import com.sharecutter.backend.dto.transaction.TransactionSearchCriteria;
import com.sharecutter.backend.exception.InvalidTransactionException;
import com.sharecutter.backend.exception.TransactionNotFoundException;
import com.sharecutter.backend.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PortfolioService portfolioService;
    private final AssetService assetService;
    private final TransactionSearchService transactionSearchService;

    public TransactionService(
            TransactionRepository transactionRepository,
            PortfolioService portfolioService,
            AssetService assetService
    ) {
        this(
                transactionRepository,
                portfolioService,
                assetService,
                null
        );
    }

    @Autowired
    public TransactionService(
            TransactionRepository transactionRepository,
            PortfolioService portfolioService,
            AssetService assetService,
            TransactionSearchService transactionSearchService
    ) {
        this.transactionRepository =
                transactionRepository;

        this.portfolioService =
                portfolioService;

        this.assetService =
                assetService;

        this.transactionSearchService =
                transactionSearchService;
    }

    @Transactional
    public TransactionEntity createTransaction(
            UUID userId,
            UUID portfolioId,
            UUID assetId,
            TransactionType transactionType,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal fee,
            BigDecimal totalAmount,
            String currency,
            OffsetDateTime executedAt,
            String notes
    ) {
        PortfolioEntity portfolio =
                portfolioService.getPortfolio(
                        userId,
                        portfolioId
                );

        AssetEntity asset =
                resolveAsset(
                        userId,
                        portfolioId,
                        assetId
                );

        validateTransaction(
                transactionType,
                asset,
                quantity,
                unitPrice,
                fee,
                totalAmount,
                executedAt
        );

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

        return transactionRepository.save(
                transaction
        );
    }

    public TransactionEntity getTransaction(
            UUID userId,
            UUID portfolioId,
            UUID transactionId
    ) {
        portfolioService.getPortfolio(
                userId,
                portfolioId
        );

        return transactionRepository
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        transactionId,
                        portfolioId
                )
                .orElseThrow(
                        () ->
                                new TransactionNotFoundException(
                                        transactionId
                                )
                );
    }

    public List<TransactionEntity> getPortfolioTransactions(
            UUID userId,
            UUID portfolioId
    ) {
        portfolioService.getPortfolio(
                userId,
                portfolioId
        );

        return transactionRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId
                );
    }

    public Page<TransactionEntity> searchTransactionsPage(
            UUID userId,
            UUID portfolioId,
            TransactionSearchCriteria criteria
    ) {
        if (transactionSearchService == null) {
            throw new IllegalStateException(
                    "Transaction search service is not configured"
            );
        }

        return transactionSearchService.searchTransactions(
                userId,
                portfolioId,
                criteria
        );
    }

    public List<TransactionEntity> searchTransactions(
            UUID userId,
            UUID portfolioId,
            UUID assetId,
            TransactionType transactionType,
            OffsetDateTime startDate,
            OffsetDateTime endDate
    ) {
        portfolioService.getPortfolio(
                userId,
                portfolioId
        );

        validateOptionalDateRange(
                startDate,
                endDate
        );

        if (assetId != null) {
            assetService.getAsset(
                    userId,
                    portfolioId,
                    assetId
            );
        }

        List<TransactionEntity> transactions =
                transactionRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                                portfolioId
                        );

        return transactions
                .stream()
                .filter(
                        transaction ->
                                matchesAsset(
                                        transaction,
                                        assetId
                                )
                )
                .filter(
                        transaction ->
                                matchesTransactionType(
                                        transaction,
                                        transactionType
                                )
                )
                .filter(
                        transaction ->
                                matchesDateRange(
                                        transaction,
                                        startDate,
                                        endDate
                                )
                )
                .toList();
    }

    public List<TransactionEntity> getAssetTransactions(
            UUID userId,
            UUID portfolioId,
            UUID assetId
    ) {
        AssetEntity asset =
                assetService.getAsset(
                        userId,
                        portfolioId,
                        assetId
                );

        return transactionRepository
                .findAllByAssetIdAndDeletedAtIsNullOrderByExecutedAtDesc(
                        asset.getId()
                );
    }

    public List<TransactionEntity> getTransactionsByType(
            UUID userId,
            UUID portfolioId,
            TransactionType transactionType
    ) {
        portfolioService.getPortfolio(
                userId,
                portfolioId
        );

        if (transactionType == null) {
            throw new InvalidTransactionException(
                    "Transaction type must not be null"
            );
        }

        return transactionRepository
                .findAllByPortfolioIdAndTransactionTypeAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId,
                        transactionType
                );
    }

    public List<TransactionEntity> getTransactionsByDateRange(
            UUID userId,
            UUID portfolioId,
            OffsetDateTime startDate,
            OffsetDateTime endDate
    ) {
        portfolioService.getPortfolio(
                userId,
                portfolioId
        );

        validateDateRange(
                startDate,
                endDate
        );

        return transactionRepository
                .findAllByPortfolioIdAndExecutedAtBetweenAndDeletedAtIsNullOrderByExecutedAtDesc(
                        portfolioId,
                        startDate,
                        endDate
                );
    }

    @Transactional
    public TransactionEntity updateTransaction(
            UUID userId,
            UUID portfolioId,
            UUID transactionId,
            UUID assetId,
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
                getTransaction(
                        userId,
                        portfolioId,
                        transactionId
                );

        AssetEntity asset =
                resolveAsset(
                        userId,
                        portfolioId,
                        assetId
                );

        validateTransaction(
                transactionType,
                asset,
                quantity,
                unitPrice,
                fee,
                totalAmount,
                executedAt
        );

        transaction.setAsset(asset);
        transaction.setTransactionType(
                transactionType
        );
        transaction.setQuantity(quantity);
        transaction.setUnitPrice(unitPrice);
        transaction.setFee(fee);
        transaction.setTotalAmount(
                totalAmount
        );
        transaction.setCurrency(currency);
        transaction.setExecutedAt(
                executedAt
        );
        transaction.setNotes(notes);

        return transactionRepository.save(
                transaction
        );
    }

    @Transactional
    public void deleteTransaction(
            UUID userId,
            UUID portfolioId,
            UUID transactionId
    ) {
        TransactionEntity transaction =
                getTransaction(
                        userId,
                        portfolioId,
                        transactionId
                );

        transaction.softDelete();

        transactionRepository.save(
                transaction
        );
    }

    private AssetEntity resolveAsset(
            UUID userId,
            UUID portfolioId,
            UUID assetId
    ) {
        if (assetId == null) {
            return null;
        }

        return assetService.getAsset(
                userId,
                portfolioId,
                assetId
        );
    }

    private boolean matchesAsset(
            TransactionEntity transaction,
            UUID assetId
    ) {
        if (assetId == null) {
            return true;
        }

        AssetEntity transactionAsset =
                transaction.getAsset();

        return transactionAsset != null
                && assetId.equals(
                        transactionAsset.getId()
                );
    }

    private boolean matchesTransactionType(
            TransactionEntity transaction,
            TransactionType transactionType
    ) {
        return transactionType == null
                || transaction.getTransactionType()
                == transactionType;
    }

    private boolean matchesDateRange(
            TransactionEntity transaction,
            OffsetDateTime startDate,
            OffsetDateTime endDate
    ) {
        if (
                startDate == null
                        && endDate == null
        ) {
            return true;
        }

        OffsetDateTime executedAt =
                transaction.getExecutedAt();

        return !executedAt.isBefore(startDate)
                && !executedAt.isAfter(endDate);
    }

    private void validateTransaction(
            TransactionType transactionType,
            AssetEntity asset,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal fee,
            BigDecimal totalAmount,
            OffsetDateTime executedAt
    ) {
        if (transactionType == null) {
            throw new InvalidTransactionException(
                    "Transaction type must not be null"
            );
        }

        validateTransactionFieldsByType(
                transactionType,
                asset,
                quantity,
                unitPrice
        );

        validateFee(
                fee,
                totalAmount
        );

        validateExecutionTime(
                executedAt
        );
    }

    private void validateTransactionFieldsByType(
            TransactionType transactionType,
            AssetEntity asset,
            BigDecimal quantity,
            BigDecimal unitPrice
    ) {
        switch (transactionType) {
            case BUY, SELL ->
                    validateTradeTransaction(
                            transactionType,
                            asset,
                            quantity,
                            unitPrice
                    );

            case DIVIDEND ->
                    validateDividendTransaction(
                            asset
                    );

            case FEE, DEPOSIT, WITHDRAWAL ->
                    validateCashTransaction(
                            transactionType,
                            asset
                    );
        }
    }

    private void validateTradeTransaction(
            TransactionType transactionType,
            AssetEntity asset,
            BigDecimal quantity,
            BigDecimal unitPrice
    ) {
        if (asset == null) {
            throw new InvalidTransactionException(
                    transactionType
                            + " transaction must reference an asset"
            );
        }

        if (quantity == null) {
            throw new InvalidTransactionException(
                    transactionType
                            + " transaction must include a quantity"
            );
        }

        if (unitPrice == null) {
            throw new InvalidTransactionException(
                    transactionType
                            + " transaction must include a unit price"
            );
        }
    }

    private void validateDividendTransaction(
            AssetEntity asset
    ) {
        if (asset == null) {
            throw new InvalidTransactionException(
                    "DIVIDEND transaction must reference an asset"
            );
        }
    }

    private void validateCashTransaction(
            TransactionType transactionType,
            AssetEntity asset
    ) {
        if (asset != null) {
            throw new InvalidTransactionException(
                    transactionType
                            + " transaction must not reference an asset"
            );
        }
    }

    private void validateFee(
            BigDecimal fee,
            BigDecimal totalAmount
    ) {
        if (
                fee == null
                        || totalAmount == null
        ) {
            return;
        }

        if (fee.compareTo(totalAmount) > 0) {
            throw new InvalidTransactionException(
                    "Transaction fee must not exceed total amount"
            );
        }
    }

    private void validateExecutionTime(
            OffsetDateTime executedAt
    ) {
        if (executedAt == null) {
            return;
        }

        OffsetDateTime currentTime =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                );

        if (executedAt.isAfter(currentTime)) {
            throw new InvalidTransactionException(
                    "Transaction execution time must not be in the future"
            );
        }
    }

    private void validateOptionalDateRange(
            OffsetDateTime startDate,
            OffsetDateTime endDate
    ) {
        if (
                startDate == null
                        && endDate == null
        ) {
            return;
        }

        if (
                startDate == null
                        || endDate == null
        ) {
            throw new InvalidTransactionException(
                    "Transaction start date and end date "
                            + "must be provided together"
            );
        }

        validateDateRange(
                startDate,
                endDate
        );
    }

    private void validateDateRange(
            OffsetDateTime startDate,
            OffsetDateTime endDate
    ) {
        if (startDate == null) {
            throw new InvalidTransactionException(
                    "Transaction start date must not be null"
            );
        }

        if (endDate == null) {
            throw new InvalidTransactionException(
                    "Transaction end date must not be null"
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new InvalidTransactionException(
                    "Transaction start date must not be after end date"
            );
        }
    }
}