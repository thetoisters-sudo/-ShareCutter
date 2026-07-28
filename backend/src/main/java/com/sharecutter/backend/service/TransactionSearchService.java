package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.dto.transaction.TransactionSearchCriteria;
import com.sharecutter.backend.exception.InvalidTransactionException;
import com.sharecutter.backend.repository.TransactionRepository;
import com.sharecutter.backend.repository.specification.TransactionSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class TransactionSearchService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 200;

    private final TransactionRepository transactionRepository;
    private final PortfolioService portfolioService;
    private final AssetService assetService;

    public TransactionSearchService(
            TransactionRepository transactionRepository,
            PortfolioService portfolioService,
            AssetService assetService
    ) {
        this.transactionRepository =
                transactionRepository;

        this.portfolioService =
                portfolioService;

        this.assetService =
                assetService;
    }

    @Transactional(readOnly = true)
    public Page<TransactionEntity> searchTransactions(
            UUID userId,
            UUID portfolioId,
            TransactionSearchCriteria criteria
    ) {
        validateRequiredIdentifiers(
                userId,
                portfolioId
        );

        portfolioService.getPortfolio(
                userId,
                portfolioId
        );

        TransactionSearchCriteria normalizedCriteria =
                normalizeCriteria(criteria);

        validateDateRange(
                normalizedCriteria.startDate(),
                normalizedCriteria.endDate()
        );

        validateAsset(
                userId,
                portfolioId,
                normalizedCriteria.assetId()
        );

        Specification<TransactionEntity> specification =
                buildSpecification(
                        portfolioId,
                        normalizedCriteria
                );

        Pageable pageable =
                buildPageable(
                        normalizedCriteria
                );

        return transactionRepository.findAll(
                specification,
                pageable
        );
    }

    private Specification<TransactionEntity>
    buildSpecification(
            UUID portfolioId,
            TransactionSearchCriteria criteria
    ) {
        Specification<TransactionEntity> specification =
                TransactionSpecifications
                        .belongsToPortfolio(
                                portfolioId
                        )
                        .and(
                                TransactionSpecifications
                                        .isNotDeleted()
                        );

        if (criteria.assetId() != null) {
            specification =
                    specification.and(
                            TransactionSpecifications
                                    .belongsToAsset(
                                            criteria.assetId()
                                    )
                    );
        }

        if (criteria.transactionType() != null) {
            specification =
                    specification.and(
                            TransactionSpecifications
                                    .hasTransactionType(
                                            criteria.transactionType()
                                    )
                    );
        }

        if (criteria.startDate() != null) {
            specification =
                    specification.and(
                            TransactionSpecifications
                                    .executedAtOrAfter(
                                            criteria.startDate()
                                    )
                    );
        }

        if (criteria.endDate() != null) {
            specification =
                    specification.and(
                            TransactionSpecifications
                                    .executedAtOrBefore(
                                            criteria.endDate()
                                    )
                    );
        }

        return specification;
    }

    private Pageable buildPageable(
            TransactionSearchCriteria criteria
    ) {
        return PageRequest.of(
                criteria.page(),
                criteria.size(),
                Sort.by(
                        Sort.Direction.DESC,
                        "executedAt"
                )
        );
    }

    private TransactionSearchCriteria normalizeCriteria(
            TransactionSearchCriteria criteria
    ) {
        if (criteria == null) {
            return new TransactionSearchCriteria(
                    null,
                    null,
                    null,
                    null,
                    DEFAULT_PAGE,
                    DEFAULT_SIZE
            );
        }

        int page =
                criteria.page() == null
                        ? DEFAULT_PAGE
                        : criteria.page();

        int size =
                criteria.size() == null
                        ? DEFAULT_SIZE
                        : criteria.size();

        validatePagination(
                page,
                size
        );

        return new TransactionSearchCriteria(
                criteria.assetId(),
                criteria.transactionType(),
                criteria.startDate(),
                criteria.endDate(),
                page,
                size
        );
    }

    private void validateRequiredIdentifiers(
            UUID userId,
            UUID portfolioId
    ) {
        if (userId == null) {
            throw new IllegalArgumentException(
                    "User identifier must not be null"
            );
        }

        if (portfolioId == null) {
            throw new IllegalArgumentException(
                    "Portfolio identifier must not be null"
            );
        }
    }

    private void validateAsset(
            UUID userId,
            UUID portfolioId,
            UUID assetId
    ) {
        if (assetId == null) {
            return;
        }

        assetService.getAsset(
                userId,
                portfolioId,
                assetId
        );
    }

    private void validateDateRange(
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

        if (startDate.isAfter(endDate)) {
            throw new InvalidTransactionException(
                    "Transaction start date must not be after end date"
            );
        }
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new InvalidTransactionException(
                    "Transaction page index must be zero or greater"
            );
        }

        if (size < 1) {
            throw new InvalidTransactionException(
                    "Transaction page size must be at least 1"
            );
        }

        if (size > MAX_SIZE) {
            throw new InvalidTransactionException(
                    "Transaction page size must not exceed "
                            + MAX_SIZE
            );
        }
    }
}