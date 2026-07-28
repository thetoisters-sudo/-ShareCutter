package com.sharecutter.backend.repository.specification;

import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.domain.enums.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<TransactionEntity>
    belongsToPortfolio(
            UUID portfolioId
    ) {
        return (
                root,
                query,
                criteriaBuilder
        ) -> criteriaBuilder.equal(
                root.get("portfolio").get("id"),
                portfolioId
        );
    }

    public static Specification<TransactionEntity>
    belongsToAsset(
            UUID assetId
    ) {
        return (
                root,
                query,
                criteriaBuilder
        ) -> criteriaBuilder.equal(
                root.get("asset").get("id"),
                assetId
        );
    }

    public static Specification<TransactionEntity>
    hasTransactionType(
            TransactionType transactionType
    ) {
        return (
                root,
                query,
                criteriaBuilder
        ) -> criteriaBuilder.equal(
                root.get("transactionType"),
                transactionType
        );
    }

    public static Specification<TransactionEntity>
    executedAtOrAfter(
            OffsetDateTime startDate
    ) {
        return (
                root,
                query,
                criteriaBuilder
        ) -> criteriaBuilder.greaterThanOrEqualTo(
                root.get("executedAt"),
                startDate
        );
    }

    public static Specification<TransactionEntity>
    executedAtOrBefore(
            OffsetDateTime endDate
    ) {
        return (
                root,
                query,
                criteriaBuilder
        ) -> criteriaBuilder.lessThanOrEqualTo(
                root.get("executedAt"),
                endDate
        );
    }

    public static Specification<TransactionEntity>
    isNotDeleted() {
        return (
                root,
                query,
                criteriaBuilder
        ) -> criteriaBuilder.isNull(
                root.get("deletedAt")
        );
    }
}