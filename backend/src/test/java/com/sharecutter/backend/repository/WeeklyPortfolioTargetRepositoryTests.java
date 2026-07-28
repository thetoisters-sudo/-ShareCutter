package com.sharecutter.backend.repository;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.entity.WeeklyPortfolioTargetEntity;
import com.sharecutter.backend.domain.enums.AssetType;
import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class WeeklyPortfolioTargetRepositoryTests {

    @Autowired
    private WeeklyPortfolioTargetRepository
            weeklyPortfolioTargetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Test
    void shouldSaveAndFindWeeklyPortfolioTarget() {
        UserEntity user = saveUser(
                "weekly.target.find@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Weekly Target Portfolio"
        );

        AssetEntity asset = saveAsset(
                portfolio,
                "AAPL",
                "Apple Inc."
        );

        LocalDate weekStartDate =
                LocalDate.of(
                        2026,
                        7,
                        27
                );

        WeeklyPortfolioTargetEntity target =
                saveWeeklyTarget(
                        portfolio,
                        asset,
                        weekStartDate,
                        "40.0000"
                );

        Optional<WeeklyPortfolioTargetEntity> result =
                weeklyPortfolioTargetRepository
                        .findByIdAndPortfolioUserIdAndDeletedAtIsNull(
                                target.getId(),
                                user.getId()
                        );

        assertTrue(result.isPresent());

        WeeklyPortfolioTargetEntity foundTarget =
                result.get();

        assertEquals(
                target.getId(),
                foundTarget.getId()
        );

        assertEquals(
                portfolio.getId(),
                foundTarget.getPortfolio().getId()
        );

        assertEquals(
                asset.getId(),
                foundTarget.getAsset().getId()
        );

        assertEquals(
                weekStartDate,
                foundTarget.getWeekStartDate()
        );

        assertBigDecimalEquals(
                "40.0000",
                foundTarget.getTargetPercentage()
        );

        assertFalse(foundTarget.isDeleted());
    }

    @Test
    void shouldFindTargetByPortfolioAssetAndWeek() {
        UserEntity user = saveUser(
                "weekly.target.composite@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Composite Lookup Portfolio"
        );

        AssetEntity asset = saveAsset(
                portfolio,
                "MSFT",
                "Microsoft Corporation"
        );

        LocalDate weekStartDate =
                LocalDate.of(
                        2026,
                        8,
                        3
                );

        WeeklyPortfolioTargetEntity target =
                saveWeeklyTarget(
                        portfolio,
                        asset,
                        weekStartDate,
                        "35.0000"
                );

        Optional<WeeklyPortfolioTargetEntity> result =
                weeklyPortfolioTargetRepository
                        .findByPortfolioIdAndAssetIdAndWeekStartDateAndDeletedAtIsNull(
                                portfolio.getId(),
                                asset.getId(),
                                weekStartDate
                        );

        assertTrue(result.isPresent());

        assertEquals(
                target.getId(),
                result.get().getId()
        );
    }

    @Test
    void shouldReturnWeeklyTargetsOrderedByAssetSymbol() {
        UserEntity user = saveUser(
                "weekly.target.order@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Ordered Targets Portfolio"
        );

        AssetEntity microsoftAsset = saveAsset(
                portfolio,
                "MSFT",
                "Microsoft Corporation"
        );

        AssetEntity appleAsset = saveAsset(
                portfolio,
                "AAPL",
                "Apple Inc."
        );

        AssetEntity nvidiaAsset = saveAsset(
                portfolio,
                "NVDA",
                "NVIDIA Corporation"
        );

        LocalDate weekStartDate =
                LocalDate.of(
                        2026,
                        8,
                        10
                );

        saveWeeklyTarget(
                portfolio,
                microsoftAsset,
                weekStartDate,
                "30.0000"
        );

        saveWeeklyTarget(
                portfolio,
                appleAsset,
                weekStartDate,
                "40.0000"
        );

        saveWeeklyTarget(
                portfolio,
                nvidiaAsset,
                weekStartDate,
                "30.0000"
        );

        List<WeeklyPortfolioTargetEntity> result =
                weeklyPortfolioTargetRepository
                        .findAllByPortfolioIdAndWeekStartDateAndDeletedAtIsNullOrderByAssetSymbolAsc(
                                portfolio.getId(),
                                weekStartDate
                        );

        assertEquals(3, result.size());

        assertEquals(
                "AAPL",
                result.get(0).getAsset().getSymbol()
        );

        assertEquals(
                "MSFT",
                result.get(1).getAsset().getSymbol()
        );

        assertEquals(
                "NVDA",
                result.get(2).getAsset().getSymbol()
        );
    }

    @Test
    void shouldReturnHistoryOrderedByWeekDescending() {
        UserEntity user = saveUser(
                "weekly.target.history@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Target History Portfolio"
        );

        AssetEntity asset = saveAsset(
                portfolio,
                "AAPL",
                "Apple Inc."
        );

        LocalDate olderWeek =
                LocalDate.of(
                        2026,
                        7,
                        27
                );

        LocalDate newerWeek =
                LocalDate.of(
                        2026,
                        8,
                        3
                );

        saveWeeklyTarget(
                portfolio,
                asset,
                olderWeek,
                "50.0000"
        );

        saveWeeklyTarget(
                portfolio,
                asset,
                newerWeek,
                "60.0000"
        );

        List<WeeklyPortfolioTargetEntity> result =
                weeklyPortfolioTargetRepository
                        .findAllByPortfolioIdAndDeletedAtIsNullOrderByWeekStartDateDescAssetSymbolAsc(
                                portfolio.getId()
                        );

        assertEquals(2, result.size());

        assertEquals(
                newerWeek,
                result.get(0).getWeekStartDate()
        );

        assertEquals(
                olderWeek,
                result.get(1).getWeekStartDate()
        );
    }

    @Test
    void shouldIgnoreSoftDeletedWeeklyTarget() {
        UserEntity user = saveUser(
                "weekly.target.deleted@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                user,
                "Deleted Target Portfolio"
        );

        AssetEntity asset = saveAsset(
                portfolio,
                "TSLA",
                "Tesla Inc."
        );

        LocalDate weekStartDate =
                LocalDate.of(
                        2026,
                        8,
                        17
                );

        WeeklyPortfolioTargetEntity target =
                saveWeeklyTarget(
                        portfolio,
                        asset,
                        weekStartDate,
                        "25.0000"
                );

        target.softDelete();

        weeklyPortfolioTargetRepository
                .saveAndFlush(target);

        Optional<WeeklyPortfolioTargetEntity> result =
                weeklyPortfolioTargetRepository
                        .findByPortfolioIdAndAssetIdAndWeekStartDateAndDeletedAtIsNull(
                                portfolio.getId(),
                                asset.getId(),
                                weekStartDate
                        );

        boolean exists =
                weeklyPortfolioTargetRepository
                        .existsByPortfolioIdAndAssetIdAndWeekStartDateAndDeletedAtIsNull(
                                portfolio.getId(),
                                asset.getId(),
                                weekStartDate
                        );

        assertTrue(result.isEmpty());
        assertFalse(exists);
    }

    @Test
    void shouldNotReturnTargetForDifferentUser() {
        UserEntity firstUser = saveUser(
                "weekly.target.first@example.com"
        );

        UserEntity secondUser = saveUser(
                "weekly.target.second@example.com"
        );

        PortfolioEntity portfolio = savePortfolio(
                firstUser,
                "Owned Target Portfolio"
        );

        AssetEntity asset = saveAsset(
                portfolio,
                "AMZN",
                "Amazon.com Inc."
        );

        WeeklyPortfolioTargetEntity target =
                saveWeeklyTarget(
                        portfolio,
                        asset,
                        LocalDate.of(
                                2026,
                                8,
                                24
                        ),
                        "100.0000"
                );

        Optional<WeeklyPortfolioTargetEntity>
                ownerResult =
                weeklyPortfolioTargetRepository
                        .findByIdAndPortfolioUserIdAndDeletedAtIsNull(
                                target.getId(),
                                firstUser.getId()
                        );

        Optional<WeeklyPortfolioTargetEntity>
                differentUserResult =
                weeklyPortfolioTargetRepository
                        .findByIdAndPortfolioUserIdAndDeletedAtIsNull(
                                target.getId(),
                                secondUser.getId()
                        );

        assertTrue(ownerResult.isPresent());
        assertTrue(differentUserResult.isEmpty());
    }

    private UserEntity saveUser(
            String email
    ) {
        UserEntity user = new UserEntity(
                email,
                "encoded-password",
                "Weekly",
                "Target"
        );

        return userRepository.saveAndFlush(user);
    }

    private PortfolioEntity savePortfolio(
            UserEntity user,
            String name
    ) {
        PortfolioEntity portfolio =
                new PortfolioEntity(
                        user,
                        name,
                        PortfolioCreationMethod.BY_AMOUNT,
                        new BigDecimal("10000.0000")
                );

        return portfolioRepository.saveAndFlush(
                portfolio
        );
    }

    private AssetEntity saveAsset(
            PortfolioEntity portfolio,
            String symbol,
            String displayName
    ) {
        AssetEntity asset = new AssetEntity(
                portfolio,
                symbol,
                displayName,
                AssetType.STOCK,
                "USD",
                null,
                "NASDAQ",
                null
        );

        return assetRepository.saveAndFlush(asset);
    }

    private WeeklyPortfolioTargetEntity
    saveWeeklyTarget(
            PortfolioEntity portfolio,
            AssetEntity asset,
            LocalDate weekStartDate,
            String targetPercentage
    ) {
        WeeklyPortfolioTargetEntity target =
                new WeeklyPortfolioTargetEntity(
                        portfolio,
                        asset,
                        weekStartDate,
                        new BigDecimal(
                                targetPercentage
                        )
                );

        return weeklyPortfolioTargetRepository
                .saveAndFlush(target);
    }

    private void assertBigDecimalEquals(
            String expected,
            BigDecimal actual
    ) {
        assertEquals(
                0,
                new BigDecimal(expected)
                        .compareTo(actual)
        );
    }
}