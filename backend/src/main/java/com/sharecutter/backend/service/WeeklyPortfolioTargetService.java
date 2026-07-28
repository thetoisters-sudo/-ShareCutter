package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.WeeklyPortfolioTargetEntity;
import com.sharecutter.backend.dto.weeklytarget.WeeklyPortfolioTargetCreateRequest;
import com.sharecutter.backend.dto.weeklytarget.WeeklyPortfolioTargetResponse;
import com.sharecutter.backend.dto.weeklytarget.WeeklyTargetItemRequest;
import com.sharecutter.backend.dto.weeklytarget.WeeklyTargetItemResponse;
import com.sharecutter.backend.exception.InvalidWeeklyPortfolioTargetException;
import com.sharecutter.backend.exception.WeeklyPortfolioTargetNotFoundException;
import com.sharecutter.backend.mapper.WeeklyPortfolioTargetMapper;
import com.sharecutter.backend.repository.WeeklyPortfolioTargetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class WeeklyPortfolioTargetService {

    private static final BigDecimal REQUIRED_TOTAL_PERCENTAGE =
            new BigDecimal("100.0000");

    private final WeeklyPortfolioTargetRepository
            weeklyPortfolioTargetRepository;

    private final PortfolioService portfolioService;
    private final AssetService assetService;

    private final WeeklyPortfolioTargetMapper
            weeklyPortfolioTargetMapper;

    public WeeklyPortfolioTargetService(
            WeeklyPortfolioTargetRepository
                    weeklyPortfolioTargetRepository,
            PortfolioService portfolioService,
            AssetService assetService,
            WeeklyPortfolioTargetMapper weeklyPortfolioTargetMapper
    ) {
        this.weeklyPortfolioTargetRepository =
                weeklyPortfolioTargetRepository;

        this.portfolioService = portfolioService;
        this.assetService = assetService;

        this.weeklyPortfolioTargetMapper =
                weeklyPortfolioTargetMapper;
    }

    @Transactional
    public WeeklyPortfolioTargetResponse replaceWeeklyTargets(
            UUID userId,
            UUID portfolioId,
            WeeklyPortfolioTargetCreateRequest request
    ) {
        validateRequest(request);

        PortfolioEntity portfolio =
                portfolioService.getPortfolio(
                        userId,
                        portfolioId
                );

        validateWeekStartDate(
                request.weekStartDate()
        );

        validateTargetItems(
                request.targets()
        );

        List<ResolvedTarget> resolvedTargets =
                resolveTargetAssets(
                        userId,
                        portfolioId,
                        request.targets()
                );

        softDeleteExistingTargets(
                portfolioId,
                request.weekStartDate()
        );

        List<WeeklyPortfolioTargetEntity> newTargets =
                resolvedTargets.stream()
                        .map(
                                resolvedTarget ->
                                        new WeeklyPortfolioTargetEntity(
                                                portfolio,
                                                resolvedTarget.asset(),
                                                request.weekStartDate(),
                                                resolvedTarget
                                                        .targetPercentage()
                                        )
                        )
                        .toList();

        List<WeeklyPortfolioTargetEntity> savedTargets =
                weeklyPortfolioTargetRepository.saveAll(
                        newTargets
                );

        weeklyPortfolioTargetRepository.flush();

        return buildResponse(
                portfolio,
                request.weekStartDate(),
                savedTargets
        );
    }

    public WeeklyPortfolioTargetResponse getWeeklyTargets(
            UUID userId,
            UUID portfolioId,
            LocalDate weekStartDate
    ) {
        PortfolioEntity portfolio =
                portfolioService.getPortfolio(
                        userId,
                        portfolioId
                );

        validateWeekStartDate(weekStartDate);

        List<WeeklyPortfolioTargetEntity> targets =
                findActiveTargets(
                        portfolioId,
                        weekStartDate
                );

        if (targets.isEmpty()) {
            throw new WeeklyPortfolioTargetNotFoundException(
                    portfolioId,
                    weekStartDate
            );
        }

        return buildResponse(
                portfolio,
                weekStartDate,
                targets
        );
    }

    public List<WeeklyPortfolioTargetResponse>
    getPortfolioTargetHistory(
            UUID userId,
            UUID portfolioId
    ) {
        PortfolioEntity portfolio =
                portfolioService.getPortfolio(
                        userId,
                        portfolioId
                );

        List<WeeklyPortfolioTargetEntity> targets =
                weeklyPortfolioTargetRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByWeekStartDateDescAssetSymbolAsc(
                                portfolioId
                        );

        return targets.stream()
                .map(
                        WeeklyPortfolioTargetEntity
                                ::getWeekStartDate
                )
                .distinct()
                .map(
                        weekStartDate ->
                                buildResponse(
                                        portfolio,
                                        weekStartDate,
                                        filterTargetsByWeek(
                                                targets,
                                                weekStartDate
                                        )
                                )
                )
                .toList();
    }

    @Transactional
    public void deleteWeeklyTargets(
            UUID userId,
            UUID portfolioId,
            LocalDate weekStartDate
    ) {
        portfolioService.getPortfolio(
                userId,
                portfolioId
        );

        validateWeekStartDate(weekStartDate);

        List<WeeklyPortfolioTargetEntity> targets =
                findActiveTargets(
                        portfolioId,
                        weekStartDate
                );

        if (targets.isEmpty()) {
            throw new WeeklyPortfolioTargetNotFoundException(
                    portfolioId,
                    weekStartDate
            );
        }

        targets.forEach(
                WeeklyPortfolioTargetEntity::softDelete
        );

        weeklyPortfolioTargetRepository.saveAll(targets);
    }

    private void validateRequest(
            WeeklyPortfolioTargetCreateRequest request
    ) {
        if (request == null) {
            throw new InvalidWeeklyPortfolioTargetException(
                    "Weekly portfolio target request must not be null"
            );
        }
    }

    private List<ResolvedTarget> resolveTargetAssets(
            UUID userId,
            UUID portfolioId,
            List<WeeklyTargetItemRequest> targetItems
    ) {
        return targetItems.stream()
                .map(
                        targetItem ->
                                new ResolvedTarget(
                                        assetService.getAsset(
                                                userId,
                                                portfolioId,
                                                targetItem.assetId()
                                        ),
                                        targetItem.targetPercentage()
                                )
                )
                .toList();
    }

    private void validateTargetItems(
            List<WeeklyTargetItemRequest> targetItems
    ) {
        if (targetItems == null || targetItems.isEmpty()) {
            throw new InvalidWeeklyPortfolioTargetException(
                    "At least one weekly target is required"
            );
        }

        Set<UUID> assetIds = new HashSet<>();
        BigDecimal totalPercentage = BigDecimal.ZERO;

        for (WeeklyTargetItemRequest targetItem : targetItems) {
            validateTargetItem(targetItem);

            if (!assetIds.add(targetItem.assetId())) {
                throw new InvalidWeeklyPortfolioTargetException(
                        "Weekly target request contains "
                                + "a duplicate asset id: "
                                + targetItem.assetId()
                );
            }

            totalPercentage = totalPercentage.add(
                    targetItem.targetPercentage()
            );
        }

        if (totalPercentage.compareTo(
                REQUIRED_TOTAL_PERCENTAGE
        ) != 0) {
            throw new InvalidWeeklyPortfolioTargetException(
                    "Weekly target percentages must total exactly "
                            + REQUIRED_TOTAL_PERCENTAGE
                            + ", but totaled "
                            + totalPercentage
            );
        }
    }

    private void validateTargetItem(
            WeeklyTargetItemRequest targetItem
    ) {
        if (targetItem == null) {
            throw new InvalidWeeklyPortfolioTargetException(
                    "Weekly target item must not be null"
            );
        }

        if (targetItem.assetId() == null) {
            throw new InvalidWeeklyPortfolioTargetException(
                    "Weekly target asset id must not be null"
            );
        }

        validateTargetPercentage(
                targetItem.targetPercentage()
        );
    }

    private void validateTargetPercentage(
            BigDecimal targetPercentage
    ) {
        if (targetPercentage == null) {
            throw new InvalidWeeklyPortfolioTargetException(
                    "Target percentage must not be null"
            );
        }

        if (targetPercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidWeeklyPortfolioTargetException(
                    "Target percentage must not be negative"
            );
        }

        if (targetPercentage.compareTo(
                REQUIRED_TOTAL_PERCENTAGE
        ) > 0) {
            throw new InvalidWeeklyPortfolioTargetException(
                    "Target percentage must not exceed 100"
            );
        }

        if (targetPercentage.scale() > 4) {
            throw new InvalidWeeklyPortfolioTargetException(
                    "Target percentage must not contain "
                            + "more than 4 decimal places"
            );
        }
    }

    private void validateWeekStartDate(
            LocalDate weekStartDate
    ) {
        if (weekStartDate == null) {
            throw new InvalidWeeklyPortfolioTargetException(
                    "Week start date must not be null"
            );
        }

        if (weekStartDate.getDayOfWeek()
                != DayOfWeek.MONDAY) {
            throw new InvalidWeeklyPortfolioTargetException(
                    "Week start date must be a Monday"
            );
        }
    }

    private List<WeeklyPortfolioTargetEntity> findActiveTargets(
            UUID portfolioId,
            LocalDate weekStartDate
    ) {
        return weeklyPortfolioTargetRepository
                .findAllByPortfolioIdAndWeekStartDateAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        portfolioId,
                        weekStartDate
                );
    }

    private void softDeleteExistingTargets(
            UUID portfolioId,
            LocalDate weekStartDate
    ) {
        List<WeeklyPortfolioTargetEntity> existingTargets =
                findActiveTargets(
                        portfolioId,
                        weekStartDate
                );

        if (existingTargets.isEmpty()) {
            return;
        }

        existingTargets.forEach(
                WeeklyPortfolioTargetEntity::softDelete
        );

        weeklyPortfolioTargetRepository.saveAll(
                existingTargets
        );

        weeklyPortfolioTargetRepository.flush();
    }

    private List<WeeklyPortfolioTargetEntity>
    filterTargetsByWeek(
            List<WeeklyPortfolioTargetEntity> targets,
            LocalDate weekStartDate
    ) {
        return targets.stream()
                .filter(
                        target ->
                                target.getWeekStartDate()
                                        .equals(weekStartDate)
                )
                .toList();
    }

    private WeeklyPortfolioTargetResponse buildResponse(
            PortfolioEntity portfolio,
            LocalDate weekStartDate,
            List<WeeklyPortfolioTargetEntity> targets
    ) {
        List<WeeklyTargetItemResponse> targetResponses =
                weeklyPortfolioTargetMapper
                        .toItemResponseList(targets);

        BigDecimal totalPercentage =
                targets.stream()
                        .map(
                                WeeklyPortfolioTargetEntity
                                        ::getTargetPercentage
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return new WeeklyPortfolioTargetResponse(
                portfolio.getId(),
                portfolio.getName(),
                weekStartDate,
                totalPercentage,
                targetResponses.size(),
                targetResponses,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    private record ResolvedTarget(
            AssetEntity asset,
            BigDecimal targetPercentage
    ) {
    }
}