package com.sharecutter.backend.controller;

import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.dto.analytics.PortfolioAllocationResponse;
import com.sharecutter.backend.dto.analytics.PortfolioSummaryResponse;
import com.sharecutter.backend.dto.analytics.TargetWeightUpdateRequest;
import com.sharecutter.backend.service.PortfolioAnalyticsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping("/allocation")
    public ResponseEntity<PortfolioAllocationResponse>
    getPortfolioAllocation(
            @AuthenticationPrincipal
            UserEntity authenticatedUser,

            @PathVariable
            UUID portfolioId
    ) {
        PortfolioAllocationResponse response =
                portfolioAnalyticsService
                        .getPortfolioAllocation(
                                authenticatedUser.getId(),
                                portfolioId
                        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping(
            "/allocation/{assetId}/target-weight"
    )
    public ResponseEntity<PortfolioAllocationResponse>
    updateTargetWeight(
            @AuthenticationPrincipal
            UserEntity authenticatedUser,

            @PathVariable
            UUID portfolioId,

            @PathVariable
            UUID assetId,

            @Valid
            @RequestBody
            TargetWeightUpdateRequest request
    ) {
        PortfolioAllocationResponse response =
                portfolioAnalyticsService
                        .updateTargetWeight(
                                authenticatedUser.getId(),
                                portfolioId,
                                assetId,
                                request.targetWeightPercent()
                        );

        return ResponseEntity.ok(
                response
        );
    }
}