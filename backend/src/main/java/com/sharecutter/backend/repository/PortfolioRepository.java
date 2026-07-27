package com.sharecutter.backend.repository;

import com.sharecutter.backend.domain.entity.PortfolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository
        extends JpaRepository<PortfolioEntity, UUID> {

    Optional<PortfolioEntity> findByIdAndDeletedAtIsNull(
            UUID id
    );

    Optional<PortfolioEntity>
    findByIdAndUserIdAndDeletedAtIsNull(
            UUID id,
            UUID userId
    );

    List<PortfolioEntity>
    findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID userId
    );

    boolean
    existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(
            UUID userId,
            String name
    );
}