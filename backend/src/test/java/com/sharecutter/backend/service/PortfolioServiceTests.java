package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
import com.sharecutter.backend.exception.PortfolioAlreadyExistsException;
import com.sharecutter.backend.exception.PortfolioNotFoundException;
import com.sharecutter.backend.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTests {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private UserService userService;

    private PortfolioService portfolioService;

    @BeforeEach
    void setUp() {
        portfolioService = new PortfolioService(
                portfolioRepository,
                userService
        );
    }

    @Test
    void shouldCreatePortfolioWithNormalizedName() {
        UUID userId = UUID.randomUUID();

        UserEntity user = createUser(
                "portfolio.user@example.com"
        );

        when(userService.getUserById(userId))
                .thenReturn(user);

        when(portfolioRepository
                .existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(
                        userId,
                        "Growth Portfolio"
                ))
                .thenReturn(false);

        when(portfolioRepository.save(any(PortfolioEntity.class)))
                .thenAnswer(
                        invocation -> invocation.getArgument(0)
                );

        PortfolioEntity result =
                portfolioService.createPortfolio(
                        userId,
                        "  Growth Portfolio  ",
                        PortfolioCreationMethod.BY_AMOUNT,
                        new BigDecimal("10000.0000")
                );

        ArgumentCaptor<PortfolioEntity> portfolioCaptor =
                ArgumentCaptor.forClass(
                        PortfolioEntity.class
                );

        verify(portfolioRepository)
                .save(portfolioCaptor.capture());

        PortfolioEntity savedPortfolio =
                portfolioCaptor.getValue();

        assertThat(result).isSameAs(savedPortfolio);

        assertThat(savedPortfolio.getUser())
                .isSameAs(user);

        assertThat(savedPortfolio.getName())
                .isEqualTo("Growth Portfolio");

        assertThat(savedPortfolio.getCreationMethod())
                .isEqualTo(
                        PortfolioCreationMethod.BY_AMOUNT
                );

        assertThat(savedPortfolio.getInitialValue())
                .isEqualByComparingTo("10000.0000");

        assertThat(savedPortfolio.getCurrentValue())
                .isEqualByComparingTo("10000.0000");

        assertThat(savedPortfolio.getTotalRealizedProfit())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(savedPortfolio.getTotalUnrealizedProfit())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(savedPortfolio.getTotalReturnPercent())
                .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(savedPortfolio.getDeletedAt()).isNull();
        assertThat(savedPortfolio.isDeleted()).isFalse();

        verify(userService).getUserById(userId);

        verify(portfolioRepository)
                .existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(
                        userId,
                        "Growth Portfolio"
                );
    }

    @Test
    void shouldRejectDuplicateActivePortfolioName() {
        UUID userId = UUID.randomUUID();

        UserEntity user = createUser(
                "existing.portfolio@example.com"
        );

        when(userService.getUserById(userId))
                .thenReturn(user);

        when(portfolioRepository
                .existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(
                        userId,
                        "Growth Portfolio"
                ))
                .thenReturn(true);

        assertThatThrownBy(
                () -> portfolioService.createPortfolio(
                        userId,
                        "  Growth Portfolio  ",
                        PortfolioCreationMethod.BY_AMOUNT,
                        new BigDecimal("10000.0000")
                )
        )
                .isInstanceOf(
                        PortfolioAlreadyExistsException.class
                )
                .hasMessageContaining(userId.toString())
                .hasMessageContaining("Growth Portfolio");

        verify(userService).getUserById(userId);

        verify(portfolioRepository)
                .existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(
                        userId,
                        "Growth Portfolio"
                );

        verify(portfolioRepository, never())
                .save(any(PortfolioEntity.class));
    }

    @Test
    void shouldReturnOwnedActivePortfolio() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "owner@example.com",
                "Income Portfolio",
                "25000.0000"
        );

        when(portfolioRepository
                .findByIdAndUserIdAndDeletedAtIsNull(
                        portfolioId,
                        userId
                ))
                .thenReturn(Optional.of(portfolio));

        PortfolioEntity result =
                portfolioService.getPortfolio(
                        userId,
                        portfolioId
                );

        assertThat(result).isSameAs(portfolio);

        verify(portfolioRepository)
                .findByIdAndUserIdAndDeletedAtIsNull(
                        portfolioId,
                        userId
                );

        verify(userService, never())
                .getUserById(any(UUID.class));
    }

    @Test
    void shouldThrowWhenOwnedActivePortfolioDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        when(portfolioRepository
                .findByIdAndUserIdAndDeletedAtIsNull(
                        portfolioId,
                        userId
                ))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> portfolioService.getPortfolio(
                        userId,
                        portfolioId
                )
        )
                .isInstanceOf(
                        PortfolioNotFoundException.class
                )
                .hasMessageContaining(
                        portfolioId.toString()
                );

        verify(portfolioRepository)
                .findByIdAndUserIdAndDeletedAtIsNull(
                        portfolioId,
                        userId
                );
    }

    @Test
    void shouldReturnAllActiveUserPortfolios() {
        UUID userId = UUID.randomUUID();

        UserEntity user = createUser(
                "portfolio.list@example.com"
        );

        PortfolioEntity firstPortfolio =
                new PortfolioEntity(
                        user,
                        "Growth Portfolio",
                        PortfolioCreationMethod.BY_AMOUNT,
                        new BigDecimal("10000.0000")
                );

        PortfolioEntity secondPortfolio =
                new PortfolioEntity(
                        user,
                        "Income Portfolio",
                        PortfolioCreationMethod.BY_HOLDINGS,
                        new BigDecimal("20000.0000")
                );

        List<PortfolioEntity> portfolios = List.of(
                secondPortfolio,
                firstPortfolio
        );

        when(userService.getUserById(userId))
                .thenReturn(user);

        when(portfolioRepository
                .findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        userId
                ))
                .thenReturn(portfolios);

        List<PortfolioEntity> result =
                portfolioService.getUserPortfolios(userId);

        assertThat(result)
                .containsExactly(
                        secondPortfolio,
                        firstPortfolio
                );

        verify(userService).getUserById(userId);

        verify(portfolioRepository)
                .findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        userId
                );
    }

    @Test
    void shouldRenameOwnedActivePortfolio() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "rename@example.com",
                "Old Portfolio",
                "15000.0000"
        );

        when(portfolioRepository
                .findByIdAndUserIdAndDeletedAtIsNull(
                        portfolioId,
                        userId
                ))
                .thenReturn(Optional.of(portfolio));

        when(portfolioRepository
                .existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(
                        userId,
                        "New Portfolio"
                ))
                .thenReturn(false);

        when(portfolioRepository.save(portfolio))
                .thenReturn(portfolio);

        PortfolioEntity result =
                portfolioService.renamePortfolio(
                        userId,
                        portfolioId,
                        "  New Portfolio  "
                );

        assertThat(result).isSameAs(portfolio);

        assertThat(portfolio.getName())
                .isEqualTo("New Portfolio");

        verify(portfolioRepository)
                .findByIdAndUserIdAndDeletedAtIsNull(
                        portfolioId,
                        userId
                );

        verify(portfolioRepository)
                .existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(
                        userId,
                        "New Portfolio"
                );

        verify(portfolioRepository).save(portfolio);
    }

    @Test
    void shouldRejectDuplicateNameWhenRenamingPortfolio() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "duplicate.rename@example.com",
                "Old Portfolio",
                "15000.0000"
        );

        when(portfolioRepository
                .findByIdAndUserIdAndDeletedAtIsNull(
                        portfolioId,
                        userId
                ))
                .thenReturn(Optional.of(portfolio));

        when(portfolioRepository
                .existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(
                        userId,
                        "Existing Portfolio"
                ))
                .thenReturn(true);

        assertThatThrownBy(
                () -> portfolioService.renamePortfolio(
                        userId,
                        portfolioId,
                        "Existing Portfolio"
                )
        )
                .isInstanceOf(
                        PortfolioAlreadyExistsException.class
                )
                .hasMessageContaining(
                        "Existing Portfolio"
                );

        assertThat(portfolio.getName())
                .isEqualTo("Old Portfolio");

        verify(portfolioRepository, never())
                .save(portfolio);
    }

    @Test
    void shouldAllowRenamingPortfolioWithSameNameIgnoringCase() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "same.name@example.com",
                "Growth Portfolio",
                "10000.0000"
        );

        when(portfolioRepository
                .findByIdAndUserIdAndDeletedAtIsNull(
                        portfolioId,
                        userId
                ))
                .thenReturn(Optional.of(portfolio));

        when(portfolioRepository.save(portfolio))
                .thenReturn(portfolio);

        PortfolioEntity result =
                portfolioService.renamePortfolio(
                        userId,
                        portfolioId,
                        "  growth portfolio  "
                );

        assertThat(result).isSameAs(portfolio);

        assertThat(portfolio.getName())
                .isEqualTo("growth portfolio");

        verify(
                portfolioRepository,
                never()
        ).existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(
                any(UUID.class),
                any(String.class)
        );

        verify(portfolioRepository).save(portfolio);
    }

    @Test
    void shouldUpdateCurrentPortfolioValueAndReturnPercent() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "value.update@example.com",
                "Growth Portfolio",
                "10000.0000"
        );

        when(portfolioRepository
                .findByIdAndUserIdAndDeletedAtIsNull(
                        portfolioId,
                        userId
                ))
                .thenReturn(Optional.of(portfolio));

        when(portfolioRepository.save(portfolio))
                .thenReturn(portfolio);

        PortfolioEntity result =
                portfolioService.updateCurrentValue(
                        userId,
                        portfolioId,
                        new BigDecimal("12500.0000")
                );

        assertThat(result).isSameAs(portfolio);

        assertThat(portfolio.getCurrentValue())
                .isEqualByComparingTo("12500.0000");

        assertThat(portfolio.getTotalReturnPercent())
                .isEqualByComparingTo("25.000000");

        verify(portfolioRepository).save(portfolio);
    }

    @Test
    void shouldSoftDeleteOwnedActivePortfolio() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "delete.portfolio@example.com",
                "Delete Portfolio",
                "5000.0000"
        );

        when(portfolioRepository
                .findByIdAndUserIdAndDeletedAtIsNull(
                        portfolioId,
                        userId
                ))
                .thenReturn(Optional.of(portfolio));

        when(portfolioRepository.save(portfolio))
                .thenReturn(portfolio);

        assertThat(portfolio.getDeletedAt()).isNull();
        assertThat(portfolio.isDeleted()).isFalse();

        portfolioService.deletePortfolio(
                userId,
                portfolioId
        );

        assertThat(portfolio.getDeletedAt()).isNotNull();
        assertThat(portfolio.isDeleted()).isTrue();

        verify(portfolioRepository)
                .findByIdAndUserIdAndDeletedAtIsNull(
                        portfolioId,
                        userId
                );

        verify(portfolioRepository).save(portfolio);

        verify(portfolioRepository, never())
                .delete(portfolio);
    }

    @Test
    void shouldThrowWhenDeletingPortfolioThatDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        when(portfolioRepository
                .findByIdAndUserIdAndDeletedAtIsNull(
                        portfolioId,
                        userId
                ))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> portfolioService.deletePortfolio(
                        userId,
                        portfolioId
                )
        )
                .isInstanceOf(
                        PortfolioNotFoundException.class
                )
                .hasMessageContaining(
                        portfolioId.toString()
                );

        verify(portfolioRepository, never())
                .save(any(PortfolioEntity.class));

        verify(portfolioRepository, never())
                .delete(any(PortfolioEntity.class));
    }

    private UserEntity createUser(String email) {
        return new UserEntity(
                email,
                "hashed-password",
                "Portfolio",
                "User"
        );
    }

    private PortfolioEntity createPortfolio(
            String userEmail,
            String portfolioName,
            String initialValue
    ) {
        UserEntity user = createUser(userEmail);

        return new PortfolioEntity(
                user,
                portfolioName,
                PortfolioCreationMethod.BY_AMOUNT,
                new BigDecimal(initialValue)
        );
    }
}