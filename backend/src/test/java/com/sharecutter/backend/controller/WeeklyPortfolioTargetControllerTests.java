package com.sharecutter.backend.controller;

import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.dto.weeklytarget.WeeklyPortfolioTargetCreateRequest;
import com.sharecutter.backend.dto.weeklytarget.WeeklyPortfolioTargetResponse;
import com.sharecutter.backend.dto.weeklytarget.WeeklyTargetItemRequest;
import com.sharecutter.backend.service.WeeklyPortfolioTargetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyPortfolioTargetControllerTests {

    @Mock
    private WeeklyPortfolioTargetService
            weeklyPortfolioTargetService;

    @Mock
    private UserEntity authenticatedUser;

    private WeeklyPortfolioTargetController
            weeklyPortfolioTargetController;

    @BeforeEach
    void setUp() {
        weeklyPortfolioTargetController =
                new WeeklyPortfolioTargetController(
                        weeklyPortfolioTargetService
                );
    }

    @Test
    void shouldReplaceWeeklyTargets() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

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
                                        UUID.randomUUID(),
                                        new BigDecimal(
                                                "100.0000"
                                        )
                                )
                        )
                );

        WeeklyPortfolioTargetResponse serviceResponse =
                createResponse(
                        portfolioId,
                        weekStartDate
                );

        when(authenticatedUser.getId())
                .thenReturn(userId);

        when(weeklyPortfolioTargetService
                .replaceWeeklyTargets(
                        userId,
                        portfolioId,
                        request
                ))
                .thenReturn(serviceResponse);

        ResponseEntity<
                WeeklyPortfolioTargetResponse
                > response =
                weeklyPortfolioTargetController
                        .replaceWeeklyTargets(
                                authenticatedUser,
                                portfolioId,
                                request
                        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .isSameAs(serviceResponse);

        verify(weeklyPortfolioTargetService)
                .replaceWeeklyTargets(
                        userId,
                        portfolioId,
                        request
                );
    }

    @Test
    void shouldReturnWeeklyTargetsForRequestedWeek() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        LocalDate weekStartDate =
                LocalDate.of(
                        2026,
                        8,
                        3
                );

        WeeklyPortfolioTargetResponse serviceResponse =
                createResponse(
                        portfolioId,
                        weekStartDate
                );

        when(authenticatedUser.getId())
                .thenReturn(userId);

        when(weeklyPortfolioTargetService
                .getWeeklyTargets(
                        userId,
                        portfolioId,
                        weekStartDate
                ))
                .thenReturn(serviceResponse);

        ResponseEntity<
                WeeklyPortfolioTargetResponse
                > response =
                weeklyPortfolioTargetController
                        .getWeeklyTargets(
                                authenticatedUser,
                                portfolioId,
                                weekStartDate
                        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .isSameAs(serviceResponse);

        verify(weeklyPortfolioTargetService)
                .getWeeklyTargets(
                        userId,
                        portfolioId,
                        weekStartDate
                );
    }

    @Test
    void shouldReturnPortfolioTargetHistory() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        WeeklyPortfolioTargetResponse firstWeek =
                createResponse(
                        portfolioId,
                        LocalDate.of(
                                2026,
                                8,
                                10
                        )
                );

        WeeklyPortfolioTargetResponse secondWeek =
                createResponse(
                        portfolioId,
                        LocalDate.of(
                                2026,
                                8,
                                3
                        )
                );

        List<WeeklyPortfolioTargetResponse>
                serviceResponse =
                List.of(
                        firstWeek,
                        secondWeek
                );

        when(authenticatedUser.getId())
                .thenReturn(userId);

        when(weeklyPortfolioTargetService
                .getPortfolioTargetHistory(
                        userId,
                        portfolioId
                ))
                .thenReturn(serviceResponse);

        ResponseEntity<
                List<WeeklyPortfolioTargetResponse>
                > response =
                weeklyPortfolioTargetController
                        .getTargetHistory(
                                authenticatedUser,
                                portfolioId
                        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .isSameAs(serviceResponse);

        assertThat(response.getBody())
                .hasSize(2);

        verify(weeklyPortfolioTargetService)
                .getPortfolioTargetHistory(
                        userId,
                        portfolioId
                );
    }

    @Test
    void shouldDeleteWeeklyTargets() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        LocalDate weekStartDate =
                LocalDate.of(
                        2026,
                        8,
                        17
                );

        when(authenticatedUser.getId())
                .thenReturn(userId);

        ResponseEntity<Void> response =
                weeklyPortfolioTargetController
                        .deleteWeeklyTargets(
                                authenticatedUser,
                                portfolioId,
                                weekStartDate
                        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(response.getBody())
                .isNull();

        verify(weeklyPortfolioTargetService)
                .deleteWeeklyTargets(
                        userId,
                        portfolioId,
                        weekStartDate
                );
    }

    private WeeklyPortfolioTargetResponse
    createResponse(
            UUID portfolioId,
            LocalDate weekStartDate
    ) {
        return new WeeklyPortfolioTargetResponse(
                portfolioId,
                "Test Portfolio",
                weekStartDate,
                new BigDecimal("100.0000"),
                0,
                List.of(),
                OffsetDateTime.of(
                        2026,
                        7,
                        28,
                        9,
                        0,
                        0,
                        0,
                        ZoneOffset.UTC
                )
        );
    }
}