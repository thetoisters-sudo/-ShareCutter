package com.sharecutter.backend.controller;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.dto.asset.AssetCreateRequest;
import com.sharecutter.backend.dto.asset.AssetResponse;
import com.sharecutter.backend.dto.asset.AssetUpdateRequest;
import com.sharecutter.backend.mapper.AssetMapper;
import com.sharecutter.backend.service.AssetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/assets")
public class AssetController {

    private final AssetService assetService;
    private final AssetMapper assetMapper;

    public AssetController(
            AssetService assetService,
            AssetMapper assetMapper
    ) {
        this.assetService = assetService;
        this.assetMapper = assetMapper;
    }

    @PostMapping
    public ResponseEntity<AssetResponse> createAsset(
            @AuthenticationPrincipal UserEntity authenticatedUser,
            @PathVariable UUID portfolioId,
            @Valid @RequestBody AssetCreateRequest request
    ) {

        AssetEntity createdAsset =
                assetService.createAsset(
                        authenticatedUser.getId(),
                        portfolioId,
                        request.symbol(),
                        request.displayName(),
                        request.assetType(),
                        request.currency(),
                        request.isin(),
                        request.exchange(),
                        request.notes()
                );

        AssetResponse response =
                assetMapper.toResponse(createdAsset);

        URI location = URI.create(
                "/api/v1/portfolios/"
                        + portfolioId
                        + "/assets/"
                        + response.id()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<AssetResponse>> getAssets(
            @AuthenticationPrincipal UserEntity authenticatedUser,
            @PathVariable UUID portfolioId
    ) {

        List<AssetEntity> assets =
                assetService.getPortfolioAssets(
                        authenticatedUser.getId(),
                        portfolioId
                );

        return ResponseEntity.ok(
                assetMapper.toResponseList(assets)
        );
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<AssetResponse> getAsset(
            @AuthenticationPrincipal UserEntity authenticatedUser,
            @PathVariable UUID portfolioId,
            @PathVariable UUID assetId
    ) {

        AssetEntity asset =
                assetService.getAsset(
                        authenticatedUser.getId(),
                        portfolioId,
                        assetId
                );

        return ResponseEntity.ok(
                assetMapper.toResponse(asset)
        );
    }

    @PutMapping("/{assetId}")
    public ResponseEntity<AssetResponse> updateAsset(
            @AuthenticationPrincipal UserEntity authenticatedUser,
            @PathVariable UUID portfolioId,
            @PathVariable UUID assetId,
            @Valid @RequestBody AssetUpdateRequest request
    ) {

        AssetEntity updatedAsset =
                assetService.updateAsset(
                        authenticatedUser.getId(),
                        portfolioId,
                        assetId,
                        request.symbol(),
                        request.displayName(),
                        request.assetType(),
                        request.currency(),
                        request.isin(),
                        request.exchange(),
                        request.notes()
                );

        return ResponseEntity.ok(
                assetMapper.toResponse(updatedAsset)
        );
    }

    @DeleteMapping("/{assetId}")
    public ResponseEntity<Void> deleteAsset(
            @AuthenticationPrincipal UserEntity authenticatedUser,
            @PathVariable UUID portfolioId,
            @PathVariable UUID assetId
    ) {

        assetService.deleteAsset(
                authenticatedUser.getId(),
                portfolioId,
                assetId
        );

        return ResponseEntity.noContent().build();
    }
}