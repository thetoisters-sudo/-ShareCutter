package com.sharecutter.backend.controller;

import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.dto.allocationpurchase.AllocationPurchaseExecuteRequest;
import com.sharecutter.backend.dto.allocationpurchase.AllocationPurchaseExecutionResponse;
import com.sharecutter.backend.dto.allocationpurchase.AllocationPurchasePreviewRequest;
import com.sharecutter.backend.dto.allocationpurchase.AllocationPurchasePreviewResponse;
import com.sharecutter.backend.service.PortfolioAllocationPurchaseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/portfolios/{portfolioId}/allocation-purchases"
)
public class PortfolioAllocationPurchaseController {

    private final PortfolioAllocationPurchaseService
            portfolioAllocationPurchaseService;

    public PortfolioAllocationPurchaseController(
            PortfolioAllocationPurchaseService
                    portfolioAllocationPurchaseService
    ) {
        this.portfolioAllocationPurchaseService =
                portfolioAllocationPurchaseService;
    }

    @PostMapping("/preview")
    public ResponseEntity<
            AllocationPurchasePreviewResponse
            > previewPurchase(
            @AuthenticationPrincipal
            UserEntity authenticatedUser,

            @PathVariable
            UUID portfolioId,

            @Valid
            @RequestBody
            AllocationPurchasePreviewRequest request
    ) {
        AllocationPurchasePreviewResponse response =
                portfolioAllocationPurchaseService
                        .previewPurchase(
                                authenticatedUser.getId(),
                                portfolioId,
                                request
                        );

        return ResponseEntity.ok(
                response
        );
    }

    @PostMapping("/execute")
    public ResponseEntity<
            AllocationPurchaseExecutionResponse
            > executePurchase(
            @AuthenticationPrincipal
            UserEntity authenticatedUser,

            @PathVariable
            UUID portfolioId,

            @Valid
            @RequestBody
            AllocationPurchaseExecuteRequest request
    ) {
        AllocationPurchaseExecutionResponse response =
                portfolioAllocationPurchaseService
                        .executePurchase(
                                authenticatedUser.getId(),
                                portfolioId,
                                request
                        );

        return ResponseEntity.ok(
                response
        );
    }
}