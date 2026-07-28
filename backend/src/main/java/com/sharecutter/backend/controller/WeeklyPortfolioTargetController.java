package com.sharecutter.backend.controller;

import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.dto.weeklytarget.WeeklyPortfolioTargetCreateRequest;
import com.sharecutter.backend.dto.weeklytarget.WeeklyPortfolioTargetResponse;
import com.sharecutter.backend.service.WeeklyPortfolioTargetService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/portfolios/{portfolioId}/weekly-targets"
)
public class WeeklyPortfolioTargetController {

    private final WeeklyPortfolioTargetService
            weeklyPortfolioTargetService;

    public WeeklyPortfolioTargetController(
            WeeklyPortfolioTargetService
                    weeklyPortfolioTargetService
    ) {
        this.weeklyPortfolioTargetService =
                weeklyPortfolioTargetService;
    }

    @PutMapping
    public ResponseEntity<WeeklyPortfolioTargetResponse>
    replaceWeeklyTargets(
            @AuthenticationPrincipal
            UserEntity authenticatedUser,
            @PathVariable
            UUID portfolioId,
            @Valid
            @RequestBody
            WeeklyPortfolioTargetCreateRequest request
    ) {
        WeeklyPortfolioTargetResponse response =
                weeklyPortfolioTargetService
                        .replaceWeeklyTargets(
                                authenticatedUser.getId(),
                                portfolioId,
                                request
                        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{weekStartDate}")
    public ResponseEntity<WeeklyPortfolioTargetResponse>
    getWeeklyTargets(
            @AuthenticationPrincipal
            UserEntity authenticatedUser,
            @PathVariable
            UUID portfolioId,
            @PathVariable
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate weekStartDate
    ) {
        WeeklyPortfolioTargetResponse response =
                weeklyPortfolioTargetService
                        .getWeeklyTargets(
                                authenticatedUser.getId(),
                                portfolioId,
                                weekStartDate
                        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<
            List<WeeklyPortfolioTargetResponse>
            >
    getTargetHistory(
            @AuthenticationPrincipal
            UserEntity authenticatedUser,
            @PathVariable
            UUID portfolioId
    ) {
        List<WeeklyPortfolioTargetResponse> response =
                weeklyPortfolioTargetService
                        .getPortfolioTargetHistory(
                                authenticatedUser.getId(),
                                portfolioId
                        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{weekStartDate}")
    public ResponseEntity<Void>
    deleteWeeklyTargets(
            @AuthenticationPrincipal
            UserEntity authenticatedUser,
            @PathVariable
            UUID portfolioId,
            @PathVariable
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate weekStartDate
    ) {
        weeklyPortfolioTargetService
                .deleteWeeklyTargets(
                        authenticatedUser.getId(),
                        portfolioId,
                        weekStartDate
                );

        return ResponseEntity
                .noContent()
                .build();
    }
}