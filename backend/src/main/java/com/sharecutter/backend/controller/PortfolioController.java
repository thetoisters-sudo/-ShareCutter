package com.sharecutter.backend.controller;

import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.dto.portfolio.PortfolioCreateRequest;
import com.sharecutter.backend.dto.portfolio.PortfolioRenameRequest;
import com.sharecutter.backend.dto.portfolio.PortfolioResponse;
import com.sharecutter.backend.dto.portfolio.PortfolioValueUpdateRequest;
import com.sharecutter.backend.mapper.PortfolioMapper;
import com.sharecutter.backend.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioMapper portfolioMapper;

    public PortfolioController(
            PortfolioService portfolioService,
            PortfolioMapper portfolioMapper
    ) {
        this.portfolioService = portfolioService;
        this.portfolioMapper = portfolioMapper;
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

    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getPortfolios(
            @AuthenticationPrincipal UserEntity authenticatedUser
    ) {
        List<PortfolioEntity> portfolios =
                portfolioService.getUserPortfolios(
                        authenticatedUser.getId()
                );

        List<PortfolioResponse> response =
                portfolioMapper.toResponseList(portfolios);

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
}