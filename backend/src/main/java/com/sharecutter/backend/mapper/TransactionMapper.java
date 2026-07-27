package com.sharecutter.backend.mapper;

import com.sharecutter.backend.domain.entity.TransactionEntity;
import com.sharecutter.backend.dto.transaction.TransactionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(
            source = "portfolio.id",
            target = "portfolioId"
    )
    @Mapping(
            source = "asset.id",
            target = "assetId"
    )
    TransactionResponse toResponse(
            TransactionEntity transaction
    );

    List<TransactionResponse> toResponseList(
            List<TransactionEntity> transactions
    );
}