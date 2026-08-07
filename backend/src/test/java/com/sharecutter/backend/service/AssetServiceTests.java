package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.AssetEntity;
import com.sharecutter.backend.domain.entity.PortfolioEntity;
import com.sharecutter.backend.domain.entity.PortfolioHoldingEntity;
import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.AssetType;
import com.sharecutter.backend.domain.enums.PortfolioCreationMethod;
import com.sharecutter.backend.exception.AssetAlreadyExistsException;
import com.sharecutter.backend.exception.AssetInUseException;
import com.sharecutter.backend.exception.AssetNotFoundException;
import com.sharecutter.backend.repository.AssetRepository;
import com.sharecutter.backend.repository.PortfolioHoldingRepository;
import com.sharecutter.backend.repository.TransactionRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceTests {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private PortfolioHoldingRepository portfolioHoldingRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private PortfolioHoldingCalculationService
            portfolioHoldingCalculationService;

    private AssetService assetService;

    @BeforeEach
    void setUp() {
        assetService = new AssetService(
                assetRepository,
                portfolioHoldingRepository,
                transactionRepository,
                portfolioService,
                portfolioHoldingCalculationService
        );
    }

    @Test
    void shouldCreateAssetWithNormalizedSymbol() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "asset.create@example.com",
                "Growth Portfolio"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetRepository
                .findByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                        portfolioId,
                        "AAPL"
                ))
                .thenReturn(Optional.empty());

        when(assetRepository.save(any(AssetEntity.class)))
                .thenAnswer(
                        invocation -> invocation.getArgument(0)
                );

        AssetEntity result = assetService.createAsset(
                userId,
                portfolioId,
                "  aapl  ",
                "Apple Inc.",
                AssetType.STOCK,
                "usd",
                "US0378331005",
                "NASDAQ",
                "Long-term holding"
        );

        ArgumentCaptor<AssetEntity> assetCaptor =
                ArgumentCaptor.forClass(
                        AssetEntity.class
                );

        verify(assetRepository)
                .save(assetCaptor.capture());

        AssetEntity savedAsset = assetCaptor.getValue();

        assertThat(result).isSameAs(savedAsset);

        assertThat(savedAsset.getPortfolio())
                .isSameAs(portfolio);

        assertThat(savedAsset.getSymbol())
                .isEqualTo("AAPL");

        assertThat(savedAsset.getDisplayName())
                .isEqualTo("Apple Inc.");

        assertThat(savedAsset.getAssetType())
                .isEqualTo(AssetType.STOCK);

        assertThat(savedAsset.getCurrency())
                .isEqualTo("USD");

        assertThat(savedAsset.getIsin())
                .isEqualTo("US0378331005");

        assertThat(savedAsset.getExchange())
                .isEqualTo("NASDAQ");

        assertThat(savedAsset.getNotes())
                .isEqualTo("Long-term holding");

        assertThat(savedAsset.getDeletedAt()).isNull();
        assertThat(savedAsset.isDeleted()).isFalse();

        verify(portfolioService).getPortfolio(
                userId,
                portfolioId
        );

        verify(assetRepository)
                .findByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                        portfolioId,
                        "AAPL"
                );
    }

    @Test
    void shouldRejectDuplicateActiveAssetSymbol() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "asset.duplicate@example.com",
                "Technology Portfolio"
        );

        AssetEntity existingAsset = createAsset(
                portfolio,
                "AAPL",
                "Apple Inc."
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetRepository
                .findByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                        portfolioId,
                        "AAPL"
                ))
                .thenReturn(Optional.of(existingAsset));

        assertThatThrownBy(
                () -> assetService.createAsset(
                        userId,
                        portfolioId,
                        "  aapl  ",
                        "Apple Inc.",
                        AssetType.STOCK,
                        "USD",
                        "US0378331005",
                        "NASDAQ",
                        null
                )
        )
                .isInstanceOf(
                        AssetAlreadyExistsException.class
                )
                .hasMessageContaining(
                        portfolioId.toString()
                )
                .hasMessageContaining("AAPL");

        verify(portfolioService).getPortfolio(
                userId,
                portfolioId
        );

        verify(assetRepository)
                .findByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                        portfolioId,
                        "AAPL"
                );

        verify(assetRepository, never())
                .save(any(AssetEntity.class));
    }

    @Test
    void shouldRejectBlankAssetSymbol() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "blank.symbol@example.com",
                "General Portfolio"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        assertThatThrownBy(
                () -> assetService.createAsset(
                        userId,
                        portfolioId,
                        "   ",
                        "Invalid Asset",
                        AssetType.OTHER,
                        "USD",
                        null,
                        null,
                        null
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "Asset symbol must not be blank"
                );

        verify(portfolioService).getPortfolio(
                userId,
                portfolioId
        );

        verify(assetRepository, never())
                .findByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                        any(UUID.class),
                        any(String.class)
                );

        verify(assetRepository, never())
                .save(any(AssetEntity.class));
    }

    @Test
    void shouldReturnOwnedActiveAsset() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "asset.owner@example.com",
                "Owner Portfolio"
        );

        AssetEntity asset = createAsset(
                portfolio,
                "MSFT",
                "Microsoft Corporation"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetRepository
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        assetId,
                        portfolioId
                ))
                .thenReturn(Optional.of(asset));

        AssetEntity result = assetService.getAsset(
                userId,
                portfolioId,
                assetId
        );

        assertThat(result).isSameAs(asset);

        verify(portfolioService).getPortfolio(
                userId,
                portfolioId
        );

        verify(assetRepository)
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        assetId,
                        portfolioId
                );
    }

    @Test
    void shouldThrowWhenOwnedActiveAssetDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "asset.missing@example.com",
                "Missing Asset Portfolio"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetRepository
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        assetId,
                        portfolioId
                ))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> assetService.getAsset(
                        userId,
                        portfolioId,
                        assetId
                )
        )
                .isInstanceOf(
                        AssetNotFoundException.class
                )
                .hasMessageContaining(
                        assetId.toString()
                );

        verify(portfolioService).getPortfolio(
                userId,
                portfolioId
        );

        verify(assetRepository)
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        assetId,
                        portfolioId
                );
    }

    @Test
    void shouldReturnAllActivePortfolioAssets() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "asset.list@example.com",
                "Asset List Portfolio"
        );

        AssetEntity firstAsset = createAsset(
                portfolio,
                "AAPL",
                "Apple Inc."
        );

        AssetEntity secondAsset = createAsset(
                portfolio,
                "MSFT",
                "Microsoft Corporation"
        );

        List<AssetEntity> assets = List.of(
                firstAsset,
                secondAsset
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetRepository
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                        portfolioId
                ))
                .thenReturn(assets);

        List<AssetEntity> result =
                assetService.getPortfolioAssets(
                        userId,
                        portfolioId
                );

        assertThat(result)
                .containsExactly(
                        firstAsset,
                        secondAsset
                );

        verify(portfolioService).getPortfolio(
                userId,
                portfolioId
        );

        verify(assetRepository)
                .findAllByPortfolioIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                        portfolioId
                );
    }

    @Test
    void shouldUpdateOwnedActiveAsset() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "asset.update@example.com",
                "Update Portfolio"
        );

        AssetEntity asset = createAsset(
                portfolio,
                "AAPL",
                "Apple Inc."
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetRepository
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        assetId,
                        portfolioId
                ))
                .thenReturn(Optional.of(asset));

        when(assetRepository
                .findByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                        portfolioId,
                        "MSFT"
                ))
                .thenReturn(Optional.empty());

        when(assetRepository.save(asset))
                .thenReturn(asset);

        AssetEntity result = assetService.updateAsset(
                userId,
                portfolioId,
                assetId,
                "  msft  ",
                "Microsoft Corporation",
                AssetType.STOCK,
                "usd",
                "US5949181045",
                "NASDAQ",
                "Updated asset"
        );

        assertThat(result).isSameAs(asset);

        assertThat(asset.getSymbol())
                .isEqualTo("MSFT");

        assertThat(asset.getDisplayName())
                .isEqualTo("Microsoft Corporation");

        assertThat(asset.getAssetType())
                .isEqualTo(AssetType.STOCK);

        assertThat(asset.getCurrency())
                .isEqualTo("USD");

        assertThat(asset.getIsin())
                .isEqualTo("US5949181045");

        assertThat(asset.getExchange())
                .isEqualTo("NASDAQ");

        assertThat(asset.getNotes())
                .isEqualTo("Updated asset");

        verify(assetRepository)
                .findByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                        portfolioId,
                        "MSFT"
                );

        verify(assetRepository).save(asset);
    }

    @Test
    void shouldAllowUpdatingAssetWithSameSymbolIgnoringCase() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "asset.same.symbol@example.com",
                "Same Symbol Portfolio"
        );

        AssetEntity asset = createAsset(
                portfolio,
                "AAPL",
                "Apple Inc."
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetRepository
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        assetId,
                        portfolioId
                ))
                .thenReturn(Optional.of(asset));

        when(assetRepository.save(asset))
                .thenReturn(asset);

        AssetEntity result = assetService.updateAsset(
                userId,
                portfolioId,
                assetId,
                "  aapl  ",
                "Apple Incorporated",
                AssetType.STOCK,
                "USD",
                "US0378331005",
                "NASDAQ",
                "Same symbol, updated details"
        );

        assertThat(result).isSameAs(asset);

        assertThat(asset.getSymbol())
                .isEqualTo("AAPL");

        assertThat(asset.getDisplayName())
                .isEqualTo("Apple Incorporated");

        verify(
                assetRepository,
                never()
        ).findByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                any(UUID.class),
                any(String.class)
        );

        verify(assetRepository).save(asset);
    }

    @Test
    void shouldRejectDuplicateSymbolWhenUpdatingAsset() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID existingAssetId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "asset.update.duplicate@example.com",
                "Duplicate Update Portfolio"
        );

        AssetEntity asset = createAsset(
                portfolio,
                "AAPL",
                "Apple Inc."
        );

        AssetEntity existingAsset = mock(
                AssetEntity.class
        );

        when(existingAsset.getId())
                .thenReturn(existingAssetId);

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetRepository
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        assetId,
                        portfolioId
                ))
                .thenReturn(Optional.of(asset));

        when(assetRepository
                .findByPortfolioIdAndSymbolIgnoreCaseAndDeletedAtIsNull(
                        portfolioId,
                        "MSFT"
                ))
                .thenReturn(Optional.of(existingAsset));

        assertThatThrownBy(
                () -> assetService.updateAsset(
                        userId,
                        portfolioId,
                        assetId,
                        "MSFT",
                        "Microsoft Corporation",
                        AssetType.STOCK,
                        "USD",
                        "US5949181045",
                        "NASDAQ",
                        null
                )
        )
                .isInstanceOf(
                        AssetAlreadyExistsException.class
                )
                .hasMessageContaining(
                        portfolioId.toString()
                )
                .hasMessageContaining("MSFT");

        assertThat(asset.getSymbol())
                .isEqualTo("AAPL");

        assertThat(asset.getDisplayName())
                .isEqualTo("Apple Inc.");

        verify(assetRepository, never())
                .save(asset);
    }

    @Test
    void shouldSoftDeleteOwnedActiveAssetAndItsHoldings() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "asset.delete@example.com",
                "Delete Asset Portfolio"
        );

        AssetEntity asset = createAsset(
                portfolio,
                "BTC",
                "Bitcoin"
        );

        PortfolioHoldingEntity holding =
                mock(PortfolioHoldingEntity.class);

        when(holding.getQuantity())
                .thenReturn(BigDecimal.ZERO);

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetRepository
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        assetId,
                        portfolioId
                ))
                .thenReturn(Optional.of(asset));

        when(transactionRepository
                .existsByAssetIdAndDeletedAtIsNull(
                        assetId
                ))
                .thenReturn(false);

        when(portfolioHoldingRepository
                .findAllByAssetIdAndDeletedAtIsNull(
                        assetId
                ))
                .thenReturn(List.of(holding));

        when(assetRepository.save(asset))
                .thenReturn(asset);

        assertThat(asset.getDeletedAt()).isNull();
        assertThat(asset.isDeleted()).isFalse();

        assetService.deleteAsset(
                userId,
                portfolioId,
                assetId
        );

        assertThat(asset.getDeletedAt()).isNotNull();
        assertThat(asset.isDeleted()).isTrue();

        verify(assetRepository)
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        assetId,
                        portfolioId
                );

        verify(portfolioHoldingRepository)
                .findAllByAssetIdAndDeletedAtIsNull(
                        assetId
                );

        verify(holding).softDelete();

        verify(portfolioHoldingRepository)
                .saveAll(List.of(holding));

        verify(assetRepository).save(asset);

        verify(portfolioHoldingCalculationService)
                .recalculatePortfolio(
                        userId,
                        portfolioId
                );

        verify(assetRepository, never())
                .delete(asset);
    }

    @Test
    void shouldSoftDeleteAssetWithoutHoldings() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "asset.delete.empty@example.com",
                "Delete Empty Asset Portfolio"
        );

        AssetEntity asset = createAsset(
                portfolio,
                "MSFT",
                "Microsoft Corporation"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetRepository
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        assetId,
                        portfolioId
                ))
                .thenReturn(Optional.of(asset));

        when(transactionRepository
                .existsByAssetIdAndDeletedAtIsNull(
                        assetId
                ))
                .thenReturn(false);

        when(portfolioHoldingRepository
                .findAllByAssetIdAndDeletedAtIsNull(
                        assetId
                ))
                .thenReturn(List.of());

        when(assetRepository.save(asset))
                .thenReturn(asset);

        assetService.deleteAsset(
                userId,
                portfolioId,
                assetId
        );

        assertThat(asset.isDeleted()).isTrue();

        verify(portfolioHoldingRepository, never())
                .saveAll(any());

        verify(assetRepository).save(asset);

        verify(portfolioHoldingCalculationService)
                .recalculatePortfolio(
                        userId,
                        portfolioId
                );
    }


    @Test
    void shouldRejectDeletingAssetWithActiveTransactions() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "asset.delete.transactions@example.com",
                "Protected Transaction Portfolio"
        );

        AssetEntity asset = createAsset(
                portfolio,
                "AAPL",
                "Apple Inc."
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetRepository
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        assetId,
                        portfolioId
                ))
                .thenReturn(Optional.of(asset));

        when(transactionRepository
                .existsByAssetIdAndDeletedAtIsNull(
                        assetId
                ))
                .thenReturn(true);

        assertThatThrownBy(
                () -> assetService.deleteAsset(
                        userId,
                        portfolioId,
                        assetId
                )
        )
                .isInstanceOf(
                        AssetInUseException.class
                )
                .hasMessageContaining(
                        assetId.toString()
                )
                .hasMessageContaining(
                        "active transactions still exist"
                );

        assertThat(asset.isDeleted()).isFalse();

        verify(transactionRepository)
                .existsByAssetIdAndDeletedAtIsNull(
                        assetId
                );

        verifyNoInteractions(
                portfolioHoldingRepository,
                portfolioHoldingCalculationService
        );

        verify(assetRepository, never())
                .save(asset);
    }

    @Test
    void shouldRejectDeletingAssetWithPositiveHolding() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "asset.delete.holding@example.com",
                "Protected Holding Portfolio"
        );

        AssetEntity asset = createAsset(
                portfolio,
                "MSFT",
                "Microsoft Corporation"
        );

        PortfolioHoldingEntity holding =
                mock(PortfolioHoldingEntity.class);

        when(holding.getQuantity())
                .thenReturn(
                        new BigDecimal(
                                "2.50000000"
                        )
                );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetRepository
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        assetId,
                        portfolioId
                ))
                .thenReturn(Optional.of(asset));

        when(transactionRepository
                .existsByAssetIdAndDeletedAtIsNull(
                        assetId
                ))
                .thenReturn(false);

        when(portfolioHoldingRepository
                .findAllByAssetIdAndDeletedAtIsNull(
                        assetId
                ))
                .thenReturn(
                        List.of(holding)
                );

        assertThatThrownBy(
                () -> assetService.deleteAsset(
                        userId,
                        portfolioId,
                        assetId
                )
        )
                .isInstanceOf(
                        AssetInUseException.class
                )
                .hasMessageContaining(
                        assetId.toString()
                )
                .hasMessageContaining(
                        "positive quantity"
                );

        assertThat(asset.isDeleted()).isFalse();

        verify(holding, never())
                .softDelete();

        verify(portfolioHoldingRepository, never())
                .saveAll(any());

        verify(assetRepository, never())
                .save(asset);

        verifyNoInteractions(
                portfolioHoldingCalculationService
        );
    }

    @Test
    void shouldThrowWhenDeletingAssetThatDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();

        PortfolioEntity portfolio = createPortfolio(
                "asset.delete.missing@example.com",
                "Missing Delete Portfolio"
        );

        when(portfolioService.getPortfolio(
                userId,
                portfolioId
        )).thenReturn(portfolio);

        when(assetRepository
                .findByIdAndPortfolioIdAndDeletedAtIsNull(
                        assetId,
                        portfolioId
                ))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> assetService.deleteAsset(
                        userId,
                        portfolioId,
                        assetId
                )
        )
                .isInstanceOf(
                        AssetNotFoundException.class
                )
                .hasMessageContaining(
                        assetId.toString()
                );

        verify(assetRepository, never())
                .save(any(AssetEntity.class));

        verify(assetRepository, never())
                .delete(any(AssetEntity.class));

        verifyNoInteractions(
                transactionRepository,
                portfolioHoldingRepository,
                portfolioHoldingCalculationService
        );
    }

    private UserEntity createUser(String email) {
        return new UserEntity(
                email,
                "hashed-password",
                "Asset",
                "User"
        );
    }

    private PortfolioEntity createPortfolio(
            String userEmail,
            String portfolioName
    ) {
        UserEntity user = createUser(userEmail);

        return new PortfolioEntity(
                user,
                portfolioName,
                PortfolioCreationMethod.BY_AMOUNT,
                new BigDecimal("10000.0000")
        );
    }

    private AssetEntity createAsset(
            PortfolioEntity portfolio,
            String symbol,
            String displayName
    ) {
        return new AssetEntity(
                portfolio,
                symbol,
                displayName,
                AssetType.STOCK,
                "USD",
                null,
                "NASDAQ",
                null
        );
    }
}