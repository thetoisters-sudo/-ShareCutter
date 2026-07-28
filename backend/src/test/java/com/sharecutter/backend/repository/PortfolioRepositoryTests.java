package com.sharecutter.backend.repository;

import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Test
    void shouldReturnFirstPageOfActivePortfolios() {
        UserEntity user = saveUser(
                "portfolio.pagination.first.page@example.com"
        );

        savePortfolio(user, "Alpha Portfolio", "1000.0000");
        savePortfolio(user, "Bravo Portfolio", "2000.0000");
        savePortfolio(user, "Charlie Portfolio", "3000.0000");
        savePortfolio(user, "Delta Portfolio", "4000.0000");
        savePortfolio(user, "Echo Portfolio", "5000.0000");

        Pageable pageable = PageRequest.of(
                0,
                2,
                Sort.by(
                        Sort.Direction.ASC,
                        "name"
                )
        );

        Page<PortfolioEntity> result =
                portfolioRepository
                        .findAllByUserIdAndDeletedAtIsNull(
                                user.getId(),
                                pageable
                        );

        assertEquals(0, result.getNumber());
        assertEquals(2, result.getSize());
        assertEquals(2, result.getNumberOfElements());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertTrue(result.isFirst());
        assertFalse(result.isLast());

        assertEquals(
                List.of(
                        "Alpha Portfolio",
                        "Bravo Portfolio"
                ),
                extractNames(result)
        );
    }

    @Test
    void shouldReturnSecondPageOfActivePortfolios() {
        UserEntity user = saveUser(
                "portfolio.pagination.second.page@example.com"
        );

        savePortfolio(user, "Alpha Portfolio", "1000.0000");
        savePortfolio(user, "Bravo Portfolio", "2000.0000");
        savePortfolio(user, "Charlie Portfolio", "3000.0000");
        savePortfolio(user, "Delta Portfolio", "4000.0000");
        savePortfolio(user, "Echo Portfolio", "5000.0000");

        Pageable pageable = PageRequest.of(
                1,
                2,
                Sort.by(
                        Sort.Direction.ASC,
                        "name"
                )
        );

        Page<PortfolioEntity> result =
                portfolioRepository
                        .findAllByUserIdAndDeletedAtIsNull(
                                user.getId(),
                                pageable
                        );

        assertEquals(1, result.getNumber());
        assertEquals(2, result.getSize());
        assertEquals(2, result.getNumberOfElements());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertFalse(result.isFirst());
        assertFalse(result.isLast());

        assertEquals(
                List.of(
                        "Charlie Portfolio",
                        "Delta Portfolio"
                ),
                extractNames(result)
        );
    }

    @Test
    void shouldReturnLastPageWithRemainingPortfolio() {
        UserEntity user = saveUser(
                "portfolio.pagination.last.page@example.com"
        );

        savePortfolio(user, "Alpha Portfolio", "1000.0000");
        savePortfolio(user, "Bravo Portfolio", "2000.0000");
        savePortfolio(user, "Charlie Portfolio", "3000.0000");
        savePortfolio(user, "Delta Portfolio", "4000.0000");
        savePortfolio(user, "Echo Portfolio", "5000.0000");

        Pageable pageable = PageRequest.of(
                2,
                2,
                Sort.by(
                        Sort.Direction.ASC,
                        "name"
                )
        );

        Page<PortfolioEntity> result =
                portfolioRepository
                        .findAllByUserIdAndDeletedAtIsNull(
                                user.getId(),
                                pageable
                        );

        assertEquals(2, result.getNumber());
        assertEquals(2, result.getSize());
        assertEquals(1, result.getNumberOfElements());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertFalse(result.isFirst());
        assertTrue(result.isLast());

        assertEquals(
                List.of("Echo Portfolio"),
                extractNames(result)
        );
    }

    @Test
    void shouldSortPortfoliosByNameAscending() {
        UserEntity user = saveUser(
                "portfolio.sort.ascending@example.com"
        );

        savePortfolio(user, "Zulu Portfolio", "1000.0000");
        savePortfolio(user, "Alpha Portfolio", "2000.0000");
        savePortfolio(user, "Mike Portfolio", "3000.0000");

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(
                        Sort.Direction.ASC,
                        "name"
                )
        );

        Page<PortfolioEntity> result =
                portfolioRepository
                        .findAllByUserIdAndDeletedAtIsNull(
                                user.getId(),
                                pageable
                        );

        assertEquals(
                List.of(
                        "Alpha Portfolio",
                        "Mike Portfolio",
                        "Zulu Portfolio"
                ),
                extractNames(result)
        );
    }

    @Test
    void shouldSortPortfoliosByNameDescending() {
        UserEntity user = saveUser(
                "portfolio.sort.descending@example.com"
        );

        savePortfolio(user, "Zulu Portfolio", "1000.0000");
        savePortfolio(user, "Alpha Portfolio", "2000.0000");
        savePortfolio(user, "Mike Portfolio", "3000.0000");

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(
                        Sort.Direction.DESC,
                        "name"
                )
        );

        Page<PortfolioEntity> result =
                portfolioRepository
                        .findAllByUserIdAndDeletedAtIsNull(
                                user.getId(),
                                pageable
                        );

        assertEquals(
                List.of(
                        "Zulu Portfolio",
                        "Mike Portfolio",
                        "Alpha Portfolio"
                ),
                extractNames(result)
        );
    }

    @Test
    void shouldReturnOnlyPortfoliosOwnedByRequestedUser() {
        UserEntity requestedUser = saveUser(
                "portfolio.pagination.requested.owner@example.com"
        );

        UserEntity otherUser = saveUser(
                "portfolio.pagination.other.owner@example.com"
        );

        PortfolioEntity requestedFirst = savePortfolio(
                requestedUser,
                "Requested Alpha",
                "1000.0000"
        );

        PortfolioEntity requestedSecond = savePortfolio(
                requestedUser,
                "Requested Bravo",
                "2000.0000"
        );

        savePortfolio(
                otherUser,
                "Other User Portfolio",
                "3000.0000"
        );

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(
                        Sort.Direction.ASC,
                        "name"
                )
        );

        Page<PortfolioEntity> result =
                portfolioRepository
                        .findAllByUserIdAndDeletedAtIsNull(
                                requestedUser.getId(),
                                pageable
                        );

        Set<?> returnedIds = result
                .getContent()
                .stream()
                .map(PortfolioEntity::getId)
                .collect(Collectors.toSet());

        assertEquals(2, result.getTotalElements());
        assertTrue(returnedIds.contains(requestedFirst.getId()));
        assertTrue(returnedIds.contains(requestedSecond.getId()));
        assertEquals(2, returnedIds.size());
    }

    @Test
    void shouldExcludeSoftDeletedPortfoliosFromPagedResults() {
        UserEntity user = saveUser(
                "portfolio.pagination.soft.delete@example.com"
        );

        PortfolioEntity activeFirst = savePortfolio(
                user,
                "Active Alpha",
                "1000.0000"
        );

        PortfolioEntity activeSecond = savePortfolio(
                user,
                "Active Bravo",
                "2000.0000"
        );

        PortfolioEntity deletedPortfolio = savePortfolio(
                user,
                "Deleted Charlie",
                "3000.0000"
        );

        deletedPortfolio.softDelete();
        portfolioRepository.saveAndFlush(deletedPortfolio);

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(
                        Sort.Direction.ASC,
                        "name"
                )
        );

        Page<PortfolioEntity> result =
                portfolioRepository
                        .findAllByUserIdAndDeletedAtIsNull(
                                user.getId(),
                                pageable
                        );

        Set<?> returnedIds = result
                .getContent()
                .stream()
                .map(PortfolioEntity::getId)
                .collect(Collectors.toSet());

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(returnedIds.contains(activeFirst.getId()));
        assertTrue(returnedIds.contains(activeSecond.getId()));
        assertFalse(returnedIds.contains(deletedPortfolio.getId()));
        assertTrue(
                result.getContent()
                        .stream()
                        .noneMatch(PortfolioEntity::isDeleted)
        );
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

    private List<String> extractNames(
            Page<PortfolioEntity> page
    ) {
        return page
                .getContent()
                .stream()
                .map(PortfolioEntity::getName)
                .toList();
    }
}