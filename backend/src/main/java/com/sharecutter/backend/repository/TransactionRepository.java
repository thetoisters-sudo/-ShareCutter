package com.sharecutter.backend.repository;

import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.domain.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository
        extends JpaRepository<TransactionEntity, UUID>,
        JpaSpecificationExecutor<TransactionEntity> {

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

    long countByPortfolioIdAndDeletedAtIsNull(
            UUID portfolioId
    );

    @Query("""
            select coalesce(sum(transaction.totalAmount), 0)
            from TransactionEntity transaction
            where transaction.portfolio.id = :portfolioId
              and transaction.transactionType = :transactionType
              and transaction.deletedAt is null
            """)
    BigDecimal sumTotalAmountByPortfolioIdAndTransactionType(
            @Param("portfolioId")
            UUID portfolioId,

            @Param("transactionType")
            TransactionType transactionType
    );
}