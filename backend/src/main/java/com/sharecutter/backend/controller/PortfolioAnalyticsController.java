package com.sharecutter.backend.controller;

import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.dto.analytics.PortfolioSummaryResponse;
import com.sharecutter.backend.service.PortfolioAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/portfolios/{portfolioId}/analytics"
)
public class PortfolioAnalyticsController {

    private final PortfolioAnalyticsService
            portfolioAnalyticsService;

    public PortfolioAnalyticsController(
            PortfolioAnalyticsService
                    portfolioAnalyticsService
    ) {
        this.portfolioAnalyticsService =
                portfolioAnalyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<PortfolioSummaryResponse>
    getPortfolioSummary(
            @AuthenticationPrincipal
            UserEntity authenticatedUser,

            @PathVariable
            UUID portfolioId
    ) {
        PortfolioSummaryResponse response =
                portfolioAnalyticsService
                        .getPortfolioSummary(
                                authenticatedUser.getId(),
                                portfolioId
                        );

        return ResponseEntity.ok(response);
    }
}