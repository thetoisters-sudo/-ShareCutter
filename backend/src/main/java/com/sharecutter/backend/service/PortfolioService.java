package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
import com.sharecutter.backend.exception.PortfolioAlreadyExistsException;
import com.sharecutter.backend.exception.PortfolioNotFoundException;
import com.sharecutter.backend.repository.PortfolioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final UserService userService;

    public PortfolioService(
            PortfolioRepository portfolioRepository,
            UserService userService
    ) {
        this.portfolioRepository = portfolioRepository;
        this.userService = userService;
    }

    @Transactional
    public PortfolioEntity createPortfolio(
            UUID userId,
            String name,
            PortfolioCreationMethod creationMethod,
            BigDecimal initialValue
    ) {
        UserEntity user = userService.getUserById(userId);
        String normalizedName = normalizeName(name);

        validateUniqueName(
                userId,
                normalizedName
        );

        PortfolioEntity portfolio = new PortfolioEntity(
                user,
                normalizedName,
                creationMethod,
                initialValue
        );

        return portfolioRepository.save(portfolio);
    }

    public PortfolioEntity getPortfolio(
            UUID userId,
            UUID portfolioId
    ) {
        return portfolioRepository
                .findByIdAndUserIdAndDeletedAtIsNull(
                        portfolioId,
                        userId
                )
                .orElseThrow(
                        () -> new PortfolioNotFoundException(
                                portfolioId
                        )
                );
    }

    public List<PortfolioEntity> getUserPortfolios(
            UUID userId
    ) {
        userService.getUserById(userId);

        return portfolioRepository
                .findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        userId
                );
    }

    public Page<PortfolioEntity> getUserPortfolios(
            UUID userId,
            Pageable pageable
    ) {
        userService.getUserById(userId);

        return portfolioRepository
                .findAllByUserIdAndDeletedAtIsNull(
                        userId,
                        pageable
                );
    }

    @Transactional
    public PortfolioEntity renamePortfolio(
            UUID userId,
            UUID portfolioId,
            String name
    ) {
        PortfolioEntity portfolio = getPortfolio(
                userId,
                portfolioId
        );

        String normalizedName = normalizeName(name);

        if (!portfolio.getName().equalsIgnoreCase(normalizedName)) {
            validateUniqueName(
                    userId,
                    normalizedName
            );
        }

        portfolio.setName(normalizedName);

        return portfolioRepository.save(portfolio);
    }

    @Transactional
    public PortfolioEntity updateCurrentValue(
            UUID userId,
            UUID portfolioId,
            BigDecimal currentValue
    ) {
        PortfolioEntity portfolio = getPortfolio(
                userId,
                portfolioId
        );

        portfolio.updateCurrentValue(currentValue);

        return portfolioRepository.save(portfolio);
    }

    @Transactional
    public void deletePortfolio(
            UUID userId,
            UUID portfolioId
    ) {
        PortfolioEntity portfolio = getPortfolio(
                userId,
                portfolioId
        );

        portfolio.softDelete();

        portfolioRepository.save(portfolio);
    }

    private void validateUniqueName(
            UUID userId,
            String name
    ) {
        boolean exists = portfolioRepository
                .existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(
                        userId,
                        name
                );

        if (exists) {
            throw new PortfolioAlreadyExistsException(
                    userId,
                    name
            );
        }
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Portfolio name must not be blank"
            );
        }

        return name.trim();
    }
}