package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.PortfolioSnapshotEntity;
import com.sharecutter.backend.dto.portfolio.PortfolioHistoryPointResponse;
import com.sharecutter.backend.repository.PortfolioSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PortfolioSnapshotService {

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    private final PortfolioSnapshotRepository
            portfolioSnapshotRepository;

    private final PortfolioService
            portfolioService;

    public PortfolioSnapshotService(
            PortfolioSnapshotRepository portfolioSnapshotRepository,
            PortfolioService portfolioService
    ) {
        this.portfolioSnapshotRepository =
                portfolioSnapshotRepository;

        this.portfolioService =
                portfolioService;
    }

    @Transactional
    public void recordSnapshot(
            PortfolioEntity portfolio,
            BigDecimal cashBalance,
            BigDecimal holdingsMarketValue
    ) {
        if (
                portfolio == null
                        || portfolio.getId() == null
        ) {
            return;
        }

        BigDecimal normalizedCash =
                zeroIfNull(
                        cashBalance
                );

        BigDecimal normalizedHoldings =
                zeroIfNull(
                        holdingsMarketValue
                );

        BigDecimal currentValue =
                zeroIfNull(
                        portfolio.getCurrentValue()
                );

        BigDecimal totalProfit =
                zeroIfNull(
                        portfolio
                                .getTotalRealizedProfit()
                )
                        .add(
                                zeroIfNull(
                                        portfolio
                                                .getTotalUnrealizedProfit()
                                )
                        );

        BigDecimal totalReturnPercent =
                calculateReturnPercent(
                        totalProfit,
                        portfolio.getInitialValue()
                );

        Optional<PortfolioSnapshotEntity>
                latestSnapshot =
                portfolioSnapshotRepository
                        .findTopByPortfolioIdOrderByCapturedAtDesc(
                                portfolio.getId()
                        );

        if (
                latestSnapshot.isPresent()
                        && sameValues(
                                latestSnapshot.get(),
                                currentValue,
                                normalizedCash,
                                normalizedHoldings,
                                totalProfit,
                                totalReturnPercent
                        )
        ) {
            return;
        }

        PortfolioSnapshotEntity snapshot =
                new PortfolioSnapshotEntity(
                        portfolio,
                        currentValue,
                        normalizedCash,
                        normalizedHoldings,
                        totalProfit,
                        totalReturnPercent,
                        OffsetDateTime.now(
                                ZoneOffset.UTC
                        )
                );

        portfolioSnapshotRepository.save(
                snapshot
        );
    }

    public List<PortfolioHistoryPointResponse>
    getHistory(
            UUID userId,
            UUID portfolioId
    ) {
        PortfolioEntity portfolio =
                portfolioService.getPortfolio(
                        userId,
                        portfolioId
                );

        List<PortfolioHistoryPointResponse>
                response =
                new ArrayList<>();

        response.add(
                createInitialPoint(
                        portfolio
                )
        );

        portfolioSnapshotRepository
                .findAllByPortfolioIdOrderByCapturedAtAsc(
                        portfolioId
                )
                .stream()
                .map(
                        this::toResponse
                )
                .forEach(
                        response::add
                );

        return List.copyOf(
                response
        );
    }

    private PortfolioHistoryPointResponse
    createInitialPoint(
            PortfolioEntity portfolio
    ) {
        BigDecimal initialValue =
                zeroIfNull(
                        portfolio.getInitialValue()
                );

        return new PortfolioHistoryPointResponse(
                initialValue,
                initialValue,
                ZERO,
                ZERO,
                ZERO,
                portfolio.getCreatedAt()
        );
    }

    private PortfolioHistoryPointResponse
    toResponse(
            PortfolioSnapshotEntity snapshot
    ) {
        return new PortfolioHistoryPointResponse(
                snapshot.getCurrentValue(),
                snapshot.getCashBalance(),
                snapshot.getHoldingsMarketValue(),
                snapshot.getTotalProfit(),
                snapshot.getTotalReturnPercent(),
                snapshot.getCapturedAt()
        );
    }

    private BigDecimal calculateReturnPercent(
            BigDecimal totalProfit,
            BigDecimal initialValue
    ) {
        BigDecimal normalizedInitialValue =
                zeroIfNull(
                        initialValue
                );

        if (
                normalizedInitialValue
                        .compareTo(
                                ZERO
                        ) <= 0
        ) {
            return ZERO;
        }

        return zeroIfNull(
                totalProfit
        )
                .multiply(
                        BigDecimal.valueOf(
                                100
                        )
                )
                .divide(
                        normalizedInitialValue,
                        6,
                        RoundingMode.HALF_UP
                );
    }

    private boolean sameValues(
            PortfolioSnapshotEntity snapshot,
            BigDecimal currentValue,
            BigDecimal cashBalance,
            BigDecimal holdingsMarketValue,
            BigDecimal totalProfit,
            BigDecimal totalReturnPercent
    ) {
        return sameNumber(
                snapshot.getCurrentValue(),
                currentValue
        )
                && sameNumber(
                snapshot.getCashBalance(),
                cashBalance
        )
                && sameNumber(
                snapshot.getHoldingsMarketValue(),
                holdingsMarketValue
        )
                && sameNumber(
                snapshot.getTotalProfit(),
                totalProfit
        )
                && sameNumber(
                snapshot.getTotalReturnPercent(),
                totalReturnPercent
        );
    }

    private boolean sameNumber(
            BigDecimal left,
            BigDecimal right
    ) {
        return zeroIfNull(
                left
        ).compareTo(
                zeroIfNull(
                        right
                )
        ) == 0;
    }

    private BigDecimal zeroIfNull(
            BigDecimal value
    ) {
        return value == null
                ? ZERO
                : value;
    }
}