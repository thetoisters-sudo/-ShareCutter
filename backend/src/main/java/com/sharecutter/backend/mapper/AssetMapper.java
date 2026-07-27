package com.sharecutter.backend.mapper;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.dto.asset.AssetResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AssetMapper {

    @Mapping(
            source = "portfolio.id",
            target = "portfolioId"
    )
    AssetResponse toResponse(
            AssetEntity asset
    );

    List<AssetResponse> toResponseList(
            List<AssetEntity> assets
    );
}