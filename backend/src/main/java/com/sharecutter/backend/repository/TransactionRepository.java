package com.sharecutter.backend.repository;

import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.domain.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository
        extends JpaRepository<TransactionEntity, UUID> {

    Optional<TransactionEntity>
    findByIdAndDeletedAtIsNull(
            UUID id
    );

    Optional<TransactionEntity>
    findByIdAndPortfolioIdAndDeletedAtIsNull(
            UUID id,
            UUID portfolioId
    );

    List<TransactionEntity>
    findAllByPortfolioIdAndDeletedAtIsNullOrderByExecutedAtDesc(
            UUID portfolioId
    );

    List<TransactionEntity>
    findAllByAssetIdAndDeletedAtIsNullOrderByExecutedAtDesc(
            UUID assetId
    );

    List<TransactionEntity>
    findAllByPortfolioIdAndTransactionTypeAndDeletedAtIsNullOrderByExecutedAtDesc(
            UUID portfolioId,
            TransactionType transactionType
    );

    List<TransactionEntity>
    findAllByPortfolioIdAndExecutedAtBetweenAndDeletedAtIsNullOrderByExecutedAtDesc(
            UUID portfolioId,
            OffsetDateTime startDate,
            OffsetDateTime endDate
    );

    boolean
    existsByIdAndPortfolioIdAndDeletedAtIsNull(
            UUID id,
            UUID portfolioId
    );
}