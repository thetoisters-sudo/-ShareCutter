package com.sharecutter.backend.repository;

import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PortfolioRepositoryTests {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindActivePortfolioById() {
        UserEntity user = saveUser(
                "portfolio.owner@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Long Term Portfolio",
                "10000.0000"
        );

        Optional<PortfolioEntity> result =
                portfolioRepository.findByIdAndDeletedAtIsNull(
                        portfolio.getId()
                );

        assertTrue(result.isPresent());
        assertEquals(
                portfolio.getId(),
                result.get().getId()
        );
        assertEquals(
                "Long Term Portfolio",
                result.get().getName()
        );
        assertEquals(
                0,
                new BigDecimal("10000.0000").compareTo(
                        result.get().getInitialValue()
                )
        );
        assertEquals(
                PortfolioCreationMethod.BY_AMOUNT,
                result.get().getCreationMethod()
        );
    }

    @Test
    void shouldFindPortfolioOnlyForItsOwner() {
        UserEntity owner = saveUser(
                "portfolio.real.owner@example.com"
        );

        UserEntity anotherUser = saveUser(
                "portfolio.other.user@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                owner,
                "Owner Portfolio",
                "15000.0000"
        );

        Optional<PortfolioEntity> ownerResult =
                portfolioRepository
                        .findByIdAndUserIdAndDeletedAtIsNull(
                                portfolio.getId(),
                                owner.getId()
                        );

        Optional<PortfolioEntity> anotherUserResult =
                portfolioRepository
                        .findByIdAndUserIdAndDeletedAtIsNull(
                                portfolio.getId(),
                                anotherUser.getId()
                        );

        assertTrue(ownerResult.isPresent());
        assertFalse(anotherUserResult.isPresent());
    }

    @Test
    void shouldReturnOnlyActivePortfoliosForUser() {
        UserEntity user = saveUser(
                "portfolio.list.owner@example.com"
        );

        PortfolioEntity activePortfolio = savePortfolio(
                user,
                "Active Portfolio",
                "20000.0000"
        );

        PortfolioEntity deletedPortfolio = savePortfolio(
                user,
                "Deleted Portfolio",
                "5000.0000"
        );

        deletedPortfolio.softDelete();
        portfolioRepository.saveAndFlush(deletedPortfolio);

        List<PortfolioEntity> result =
                portfolioRepository
                        .findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                                user.getId()
                        );

        assertEquals(1, result.size());
        assertEquals(
                activePortfolio.getId(),
                result.getFirst().getId()
        );
        assertFalse(result.getFirst().isDeleted());
    }

    @Test
    void shouldDetectActivePortfolioNameIgnoringCase() {
        UserEntity user = saveUser(
                "portfolio.name.owner@example.com"
        );

        savePortfolio(
                user,
                "Retirement Portfolio",
                "30000.0000"
        );

        boolean exists =
                portfolioRepository
                        .existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(
                                user.getId(),
                                "retirement portfolio"
                        );

        boolean differentNameExists =
                portfolioRepository
                        .existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(
                                user.getId(),
                                "Growth Portfolio"
                        );

        assertTrue(exists);
        assertFalse(differentNameExists);
    }

    @Test
    void shouldNotReturnSoftDeletedPortfolio() {
        UserEntity user = saveUser(
                "portfolio.deleted.owner@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Temporary Portfolio",
                "7500.0000"
        );

        portfolio.softDelete();
        portfolioRepository.saveAndFlush(portfolio);

        Optional<PortfolioEntity> result =
                portfolioRepository.findByIdAndDeletedAtIsNull(
                        portfolio.getId()
                );

        boolean nameExists =
                portfolioRepository
                        .existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(
                                user.getId(),
                                "Temporary Portfolio"
                        );

        assertTrue(result.isEmpty());
        assertFalse(nameExists);
    }

    private UserEntity saveUser(String email) {
        UserEntity user = new UserEntity(
                email,
                "encoded-password",
                "Portfolio",
                "Owner"
        );

        return userRepository.saveAndFlush(user);
    }

    private PortfolioEntity savePortfolio(
            UserEntity user,
            String name,
            String initialValue
    ) {
        PortfolioEntity portfolio = new PortfolioEntity(
                user,
                name,
                PortfolioCreationMethod.BY_AMOUNT,
                new BigDecimal(initialValue)
        );

        return portfolioRepository.saveAndFlush(portfolio);
    }
}