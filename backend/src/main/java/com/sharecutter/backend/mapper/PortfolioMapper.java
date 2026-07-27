package com.sharecutter.backend.mapper;

import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.dto.portfolio.PortfolioResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PortfolioMapper {

    @Mapping(
            source = "user.id",
            target = "userId"
    )
    PortfolioResponse toResponse(
            PortfolioEntity portfolio
    );

    List<PortfolioResponse> toResponseList(
            List<PortfolioEntity> portfolios
    );
}