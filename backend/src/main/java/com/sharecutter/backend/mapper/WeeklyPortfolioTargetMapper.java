package com.sharecutter.backend.mapper;

import com.sharecutter.backend.domain.entity.WeeklyPortfolioTargetEntity;
import com.sharecutter.backend.dto.weeklytarget.WeeklyTargetItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WeeklyPortfolioTargetMapper {

    @Mapping(
            source = "asset.id",
            target = "assetId"
    )
    @Mapping(
            source = "asset.symbol",
            target = "symbol"
    )
    @Mapping(
            source = "asset.displayName",
            target = "displayName"
    )
    WeeklyTargetItemResponse toItemResponse(
            WeeklyPortfolioTargetEntity target
    );

    List<WeeklyTargetItemResponse> toItemResponseList(
            List<WeeklyPortfolioTargetEntity> targets
    );
}