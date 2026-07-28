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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyPortfolioTargetServiceTests {

    @Mock
    private WeeklyPortfolioTargetRepository
            weeklyPortfolioTargetRepository;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private AssetService assetService;

    @Mock
    private WeeklyPortfolioTargetMapper
            weeklyPortfolioTargetMapper;

    @Mock
    private PortfolioEntity portfolio;

    @Mock
    private AssetEntity appleAsset;

    @Mock
    private AssetEntity microsoftAsset;

    private WeeklyPortfolioTargetService
            weeklyPortfolioTargetService;

    @BeforeEach
    void setUp() {
        weeklyPortfolioTargetService =
                new WeeklyPortfolioTargetService(
                        weeklyPortfolioTargetRepository,
                        portfolioService,
                        assetService,
                        weeklyPortfolioTargetMapper
                );
    }

    @Test
    void shouldReplaceWeeklyTargets() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID appleAssetId = UUID.randomUUID();
        UUID microsoftAssetId = UUID.randomUUID();

        LocalDate weekStartDate =
                LocalDate.of(
                        2026,
                        7,
                        27
                );

        WeeklyPortfolioTargetCreateRequest request =
                new WeeklyPortfolioTargetCreateRequest(
                        weekStartDate,
                        List.of(
                                new WeeklyTargetItemRequest(
                                        appleAssetId,
                                        new BigDecimal(
                                                "60.0000"
                                        )
                                ),
                                new WeeklyTargetItemRequest(
                                        microsoftAssetId,
                                        new BigDecimal(
                                                "40.0000"
                                        )
                                )
                        )
                );

        when(portfolio.getId())
                .thenReturn(portfolioId);

        when(portfolio.getName())
                .thenReturn("Growth Portfolio");

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetService.getAsset(
                userId,
                portfolioId,
                appleAssetId
        )).thenReturn(appleAsset);

        when(assetService.getAsset(
                userId,
                portfolioId,
                microsoftAssetId
        )).thenReturn(microsoftAsset);

        when(weeklyPortfolioTargetRepository
                .findAllByPortfolioIdAndWeekStartDateAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        portfolioId,
                        weekStartDate
                ))
                .thenReturn(List.of());

        when(weeklyPortfolioTargetRepository
                .saveAll(anyList()))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        WeeklyTargetItemResponse appleResponse =
                org.mockito.Mockito.mock(
                        WeeklyTargetItemResponse.class
                );

        WeeklyTargetItemResponse microsoftResponse =
                org.mockito.Mockito.mock(
                        WeeklyTargetItemResponse.class
                );

        when(weeklyPortfolioTargetMapper
                .toItemResponseList(anyList()))
                .thenReturn(
                        List.of(
                                appleResponse,
                                microsoftResponse
                        )
                );

        WeeklyPortfolioTargetResponse response =
                weeklyPortfolioTargetService
                        .replaceWeeklyTargets(
                                userId,
                                portfolioId,
                                request
                        );

        ArgumentCaptor<
                List<WeeklyPortfolioTargetEntity>
                > targetCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(weeklyPortfolioTargetRepository)
                .saveAll(targetCaptor.capture());

        List<WeeklyPortfolioTargetEntity> savedTargets =
                targetCaptor.getValue();

        assertThat(savedTargets).hasSize(2);

        assertThat(
                savedTargets.get(0).getPortfolio()
        ).isSameAs(portfolio);

        assertThat(
                savedTargets.get(0).getAsset()
        ).isSameAs(appleAsset);

        assertThat(
                savedTargets.get(0).getWeekStartDate()
        ).isEqualTo(weekStartDate);

        assertThat(
                savedTargets.get(0)
                        .getTargetPercentage()
        ).isEqualByComparingTo("60.0000");

        assertThat(
                savedTargets.get(1).getAsset()
        ).isSameAs(microsoftAsset);

        assertThat(
                savedTargets.get(1)
                        .getTargetPercentage()
        ).isEqualByComparingTo("40.0000");

        assertThat(response.portfolioId())
                .isEqualTo(portfolioId);

        assertThat(response.portfolioName())
                .isEqualTo("Growth Portfolio");

        assertThat(response.weekStartDate())
                .isEqualTo(weekStartDate);

        assertThat(response.totalTargetPercentage())
                .isEqualByComparingTo("100.0000");

        assertThat(response.targetCount())
                .isEqualTo(2);

        assertThat(response.targets())
                .hasSize(2);

        assertThat(response.generatedAt())
                .isNotNull();

        verify(weeklyPortfolioTargetRepository)
                .flush();
    }

    @Test
    void shouldSoftDeleteExistingTargetsBeforeReplacement() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        LocalDate weekStartDate =
                LocalDate.of(
                        2026,
                        8,
                        3
                );

        WeeklyPortfolioTargetEntity existingTarget =
                new WeeklyPortfolioTargetEntity(
                        portfolio,
                        appleAsset,
                        weekStartDate,
                        new BigDecimal("100.0000")
                );

        WeeklyPortfolioTargetCreateRequest request =
                new WeeklyPortfolioTargetCreateRequest(
                        weekStartDate,
                        List.of(
                                new WeeklyTargetItemRequest(
                                        assetId,
                                        new BigDecimal(
                                                "100.0000"
                                        )
                                )
                        )
                );

        when(portfolio.getId())
                .thenReturn(portfolioId);

        when(portfolio.getName())
                .thenReturn("Replacement Portfolio");

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetService.getAsset(
                userId,
                portfolioId,
                assetId
        )).thenReturn(appleAsset);

        when(weeklyPortfolioTargetRepository
                .findAllByPortfolioIdAndWeekStartDateAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        portfolioId,
                        weekStartDate
                ))
                .thenReturn(
                        List.of(existingTarget)
                );

        when(weeklyPortfolioTargetRepository
                .saveAll(anyList()))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        when(weeklyPortfolioTargetMapper
                .toItemResponseList(anyList()))
                .thenReturn(List.of());

        assertThat(existingTarget.isDeleted())
                .isFalse();

        weeklyPortfolioTargetService
                .replaceWeeklyTargets(
                        userId,
                        portfolioId,
                        request
                );

        assertThat(existingTarget.isDeleted())
                .isTrue();

        assertThat(existingTarget.getDeletedAt())
                .isNotNull();

        verify(weeklyPortfolioTargetRepository)
                .saveAll(
                        List.of(existingTarget)
                );
    }

    @Test
    void shouldRejectWeekStartDateThatIsNotMonday() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        WeeklyPortfolioTargetCreateRequest request =
                new WeeklyPortfolioTargetCreateRequest(
                        LocalDate.of(
                                2026,
                                7,
                                28
                        ),
                        List.of(
                                new WeeklyTargetItemRequest(
                                        UUID.randomUUID(),
                                        new BigDecimal(
                                                "100.0000"
                                        )
                                )
                        )
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        assertThatThrownBy(
                () -> weeklyPortfolioTargetService
                        .replaceWeeklyTargets(
                                userId,
                                portfolioId,
                                request
                        )
        )
                .isInstanceOf(
                        InvalidWeeklyPortfolioTargetException.class
                )
                .hasMessageContaining("Monday");

        verify(assetService, never())
                .getAsset(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );

        verify(
                weeklyPortfolioTargetRepository,
                never()
        ).saveAll(anyList());
    }

    @Test
    void shouldRejectTotalPercentageBelowOneHundred() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        WeeklyPortfolioTargetCreateRequest request =
                new WeeklyPortfolioTargetCreateRequest(
                        LocalDate.of(
                                2026,
                                8,
                                10
                        ),
                        List.of(
                                new WeeklyTargetItemRequest(
                                        UUID.randomUUID(),
                                        new BigDecimal(
                                                "70.0000"
                                        )
                                )
                        )
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        assertThatThrownBy(
                () -> weeklyPortfolioTargetService
                        .replaceWeeklyTargets(
                                userId,
                                portfolioId,
                                request
                        )
        )
                .isInstanceOf(
                        InvalidWeeklyPortfolioTargetException.class
                )
                .hasMessageContaining("100.0000")
                .hasMessageContaining("70.0000");

        verify(
                weeklyPortfolioTargetRepository,
                never()
        ).saveAll(anyList());
    }

    @Test
    void shouldRejectDuplicateAssetIds() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        WeeklyPortfolioTargetCreateRequest request =
                new WeeklyPortfolioTargetCreateRequest(
                        LocalDate.of(
                                2026,
                                8,
                                17
                        ),
                        List.of(
                                new WeeklyTargetItemRequest(
                                        assetId,
                                        new BigDecimal(
                                                "50.0000"
                                        )
                                ),
                                new WeeklyTargetItemRequest(
                                        assetId,
                                        new BigDecimal(
                                                "50.0000"
                                        )
                                )
                        )
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        assertThatThrownBy(
                () -> weeklyPortfolioTargetService
                        .replaceWeeklyTargets(
                                userId,
                                portfolioId,
                                request
                        )
        )
                .isInstanceOf(
                        InvalidWeeklyPortfolioTargetException.class
                )
                .hasMessageContaining("duplicate")
                .hasMessageContaining(
                        assetId.toString()
                );

        verify(assetService, never())
                .getAsset(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void shouldReturnWeeklyTargets() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        LocalDate weekStartDate =
                LocalDate.of(
                        2026,
                        8,
                        24
                );

        WeeklyPortfolioTargetEntity target =
                new WeeklyPortfolioTargetEntity(
                        portfolio,
                        appleAsset,
                        weekStartDate,
                        new BigDecimal("100.0000")
                );

        WeeklyTargetItemResponse itemResponse =
                org.mockito.Mockito.mock(
                        WeeklyTargetItemResponse.class
                );

        when(portfolio.getId())
                .thenReturn(portfolioId);

        when(portfolio.getName())
                .thenReturn("Weekly Portfolio");

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(weeklyPortfolioTargetRepository
                .findAllByPortfolioIdAndWeekStartDateAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        portfolioId,
                        weekStartDate
                ))
                .thenReturn(List.of(target));

        when(weeklyPortfolioTargetMapper
                .toItemResponseList(
                        List.of(target)
                ))
                .thenReturn(
                        List.of(itemResponse)
                );

        WeeklyPortfolioTargetResponse response =
                weeklyPortfolioTargetService
                        .getWeeklyTargets(
                                userId,
                                portfolioId,
                                weekStartDate
                        );

        assertThat(response.portfolioId())
                .isEqualTo(portfolioId);

        assertThat(response.weekStartDate())
                .isEqualTo(weekStartDate);

        assertThat(response.totalTargetPercentage())
                .isEqualByComparingTo("100.0000");

        assertThat(response.targetCount())
                .isEqualTo(1);
    }

    @Test
    void shouldThrowWhenWeeklyTargetsDoNotExist() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        LocalDate weekStartDate =
                LocalDate.of(
                        2026,
                        8,
                        31
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(weeklyPortfolioTargetRepository
                .findAllByPortfolioIdAndWeekStartDateAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        portfolioId,
                        weekStartDate
                ))
                .thenReturn(List.of());

        assertThatThrownBy(
                () -> weeklyPortfolioTargetService
                        .getWeeklyTargets(
                                userId,
                                portfolioId,
                                weekStartDate
                        )
        )
                .isInstanceOf(
                        WeeklyPortfolioTargetNotFoundException.class
                )
                .hasMessageContaining(
                        portfolioId.toString()
                )
                .hasMessageContaining(
                        weekStartDate.toString()
                );
    }

    @Test
    void shouldSoftDeleteWeeklyTargets() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        LocalDate weekStartDate =
                LocalDate.of(
                        2026,
                        9,
                        7
                );

        WeeklyPortfolioTargetEntity firstTarget =
                new WeeklyPortfolioTargetEntity(
                        portfolio,
                        appleAsset,
                        weekStartDate,
                        new BigDecimal("60.0000")
                );

        WeeklyPortfolioTargetEntity secondTarget =
                new WeeklyPortfolioTargetEntity(
                        portfolio,
                        microsoftAsset,
                        weekStartDate,
                        new BigDecimal("40.0000")
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(weeklyPortfolioTargetRepository
                .findAllByPortfolioIdAndWeekStartDateAndDeletedAtIsNullOrderByAssetSymbolAsc(
                        portfolioId,
                        weekStartDate
                ))
                .thenReturn(
                        List.of(
                                firstTarget,
                                secondTarget
                        )
                );

        weeklyPortfolioTargetService
                .deleteWeeklyTargets(
                        userId,
                        portfolioId,
                        weekStartDate
                );

        assertThat(firstTarget.isDeleted())
                .isTrue();

        assertThat(secondTarget.isDeleted())
                .isTrue();

        verify(weeklyPortfolioTargetRepository)
                .saveAll(
                        List.of(
                                firstTarget,
                                secondTarget
                        )
                );

        verify(
                weeklyPortfolioTargetRepository,
                never()
        ).deleteAll(
                org.mockito.ArgumentMatchers
                        .anyList()
        );
    }
}
