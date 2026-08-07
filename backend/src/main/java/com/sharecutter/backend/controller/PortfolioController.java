package com.sharecutter.backend.controller;

import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.dto.common.PagedResponse;
import com.sharecutter.backend.dto.marketdata.PortfolioMarketRefreshResponse;
import com.sharecutter.backend.dto.portfolio.PortfolioCreateRequest;
import com.sharecutter.backend.dto.portfolio.PortfolioRenameRequest;
import com.sharecutter.backend.dto.portfolio.PortfolioResponse;
import com.sharecutter.backend.dto.portfolio.PortfolioValueUpdateRequest;
import com.sharecutter.backend.mapper.PortfolioMapper;
import com.sharecutter.backend.service.PortfolioMarketRefreshService;
import com.sharecutter.backend.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final String DEFAULT_SORT_BY = "createdAt";
    private static final String DEFAULT_SORT_DIRECTION = "desc";

    private final PortfolioService portfolioService;
    private final PortfolioMapper portfolioMapper;
    private final PortfolioMarketRefreshService
            portfolioMarketRefreshService;

    public PortfolioController(
            PortfolioService portfolioService,
            PortfolioMapper portfolioMapper,
            PortfolioMarketRefreshService portfolioMarketRefreshService
    ) {
        this.portfolioService = portfolioService;
        this.portfolioMapper = portfolioMapper;
        this.portfolioMarketRefreshService =
                portfolioMarketRefreshService;
    }

    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(
            @AuthenticationPrincipal UserEntity authenticatedUser,
            @Valid @RequestBody PortfolioCreateRequest request
    ) {
        PortfolioEntity createdPortfolio =
                portfolioService.createPortfolio(
                        authenticatedUser.getId(),
                        request.name(),
                        request.creationMethod(),
                        request.initialValue()
                );

        PortfolioResponse response =
                portfolioMapper.toResponse(createdPortfolio);

        URI location = URI.create(
                "/api/v1/portfolios/" + response.id()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @PostMapping("/market-refresh")
    public ResponseEntity<PortfolioMarketRefreshResponse>
    refreshMarketData(
            @AuthenticationPrincipal UserEntity authenticatedUser
    ) {
        PortfolioMarketRefreshResponse response =
                portfolioMarketRefreshService
                        .refreshUserPortfolios(
                                authenticatedUser.getId()
                        );

        return ResponseEntity.ok(
                response
        );
    }

    @GetMapping
    public ResponseEntity<PagedResponse<PortfolioResponse>>
    getPortfolios(
            @AuthenticationPrincipal UserEntity authenticatedUser,
            @RequestParam(
                    defaultValue = "0"
            ) int page,
            @RequestParam(
                    defaultValue = "20"
            ) int size,
            @RequestParam(
                    defaultValue = "createdAt"
            ) String sortBy,
            @RequestParam(
                    defaultValue = "desc"
            ) String sortDirection
    ) {
        validatePagination(
                page,
                size
        );

        Sort.Direction direction =
                Sort.Direction.fromString(sortDirection);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        direction,
                        sortBy
                )
        );

        Page<PortfolioEntity> portfolios =
                portfolioService.getUserPortfolios(
                        authenticatedUser.getId(),
                        pageable
                );

        PagedResponse<PortfolioResponse> response =
                PagedResponse.from(
                        portfolios,
                        portfolioMapper::toResponse
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> getPortfolio(
            @AuthenticationPrincipal UserEntity authenticatedUser,
            @PathVariable UUID portfolioId
    ) {
        PortfolioEntity portfolio =
                portfolioService.getPortfolio(
                        authenticatedUser.getId(),
                        portfolioId
                );

        PortfolioResponse response =
                portfolioMapper.toResponse(portfolio);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{portfolioId}/name")
    public ResponseEntity<PortfolioResponse> renamePortfolio(
            @AuthenticationPrincipal UserEntity authenticatedUser,
            @PathVariable UUID portfolioId,
            @Valid @RequestBody PortfolioRenameRequest request
    ) {
        PortfolioEntity updatedPortfolio =
                portfolioService.renamePortfolio(
                        authenticatedUser.getId(),
                        portfolioId,
                        request.name()
                );

        PortfolioResponse response =
                portfolioMapper.toResponse(updatedPortfolio);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{portfolioId}/value")
    public ResponseEntity<PortfolioResponse> updatePortfolioValue(
            @AuthenticationPrincipal UserEntity authenticatedUser,
            @PathVariable UUID portfolioId,
            @Valid @RequestBody PortfolioValueUpdateRequest request
    ) {
        PortfolioEntity updatedPortfolio =
                portfolioService.updateCurrentValue(
                        authenticatedUser.getId(),
                        portfolioId,
                        request.currentValue()
                );

        PortfolioResponse response =
                portfolioMapper.toResponse(updatedPortfolio);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<Void> deletePortfolio(
            @AuthenticationPrincipal UserEntity authenticatedUser,
            @PathVariable UUID portfolioId
    ) {
        portfolioService.deletePortfolio(
                authenticatedUser.getId(),
                portfolioId
        );

        return ResponseEntity.noContent().build();
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (page < DEFAULT_PAGE) {
            throw new IllegalArgumentException(
                    "Page number must not be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }
    }
}