import {
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
} from '@angular/common';
import {
    HttpErrorResponse,
} from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    DestroyRef,
    OnInit,
    inject,
} from '@angular/core';
import {
    takeUntilDestroyed,
} from '@angular/core/rxjs-interop';
import {
    FormsModule,
} from '@angular/forms';
import {
    Router,
} from '@angular/router';
import {
    Observable,
    catchError,
    forkJoin,
    map,
    of,
    switchMap,
} from 'rxjs';

import {
    AssetCreateRequest,
    AssetResponse,
    AssetType,
} from '../../../../core/asset/models/asset.models';
import {
    AssetService,
} from '../../../../core/asset/services/asset.service';
import {
    TranslationService,
} from '../../../../core/i18n/services/translation.service';
import {
    MarketSymbolSearchResponse,
} from '../../../../core/market-data/models/market-data.models';
import {
    MarketDataService,
} from '../../../../core/market-data/services/market-data.service';
import {
    PagedResponse,
    PortfolioAllocationResponse,
    PortfolioCreateFromHoldingsRequest,
    PortfolioCreateRequest,
    PortfolioCreationMethod,
    PortfolioHoldingCreateItemRequest,
    PortfolioResponse,
    PortfolioSummaryResponse,
} from '../../../../core/portfolio/models/portfolio.models';
import {
    PortfolioChangeEvent,
    PortfolioService,
} from '../../../../core/portfolio/services/portfolio.service';
import {
    TransactionCreateRequest,
} from '../../../../core/transaction/models/transaction.models';
import {
    TransactionService,
} from '../../../../core/transaction/services/transaction.service';

type PortfolioDialogMode =
    | 'create'
    | 'edit'
    | 'delete'
    | null;

type PortfolioAction =
    | 'create'
    | 'rename'
    | 'delete';

interface PortfolioListItem {
    id: string;
    userId: string;
    name: string;
    creationMethod: PortfolioCreationMethod;
    initialValue: number;
    currentValue: number;
    totalRealizedProfit: number;
    totalUnrealizedProfit: number;
    totalProfit: number;
    totalReturnPercent: number;
    activeAssetCount: number;
    transactionCount: number;
    calculatedAt: string;
    createdAt: string;
    updatedAt: string;
    analyticsAvailable: boolean;
}

interface PortfolioPageResult {
    response:
    PagedResponse<PortfolioResponse>;
    portfolios:
    PortfolioListItem[];
}

interface HoldingDraft {
    symbol: string;
    displayName: string;
    assetType: AssetType;
    currency: string;
    exchange: string | null;
    quantity: number;
}

interface EditHolding {
    assetId: string;
    symbol: string;
    displayName: string;
    assetType: string;
    currency: string;
    quantity: number;
    desiredQuantity: number | string | null;
    currentPrice: number;
    isSubmitting: boolean;
    errorMessage: string;
}

type EditAssetInputMode =
    | 'BY_QUANTITY'
    | 'BY_AMOUNT';

@Component({
    selector: 'app-portfolios-page',
    imports: [
        CurrencyPipe,
        DatePipe,
        DecimalPipe,
        FormsModule,
    ],
    templateUrl:
        './portfolios-page.html',
    styleUrl:
        './portfolios-page.scss',
    changeDetection:
        ChangeDetectionStrategy.OnPush,
})
export class PortfoliosPage
    implements OnInit {

    private readonly portfolioService =
        inject(PortfolioService);

    private readonly marketDataService =
        inject(MarketDataService);

    private readonly assetService =
        inject(AssetService);

    private readonly transactionService =
        inject(TransactionService);

    private readonly router =
        inject(Router);

    private readonly translationService =
        inject(TranslationService);

    private readonly changeDetectorRef =
        inject(ChangeDetectorRef);

    private readonly destroyRef =
        inject(DestroyRef);

    protected readonly text =
        this.translationService.text;

    protected portfolios:
        PortfolioListItem[] = [];

    protected currentPage = 0;
    protected pageSize = 6;
    protected totalElements = 0;
    protected totalPages = 0;
    protected isFirstPage = true;
    protected isLastPage = true;

    protected isLoading = true;
    protected isSubmitting = false;
    protected errorMessage = '';
    protected actionErrorMessage = '';
    protected successMessage = '';

    protected dialogMode:
        PortfolioDialogMode = null;

    protected selectedPortfolio:
        PortfolioListItem | null = null;

    protected createName = '';

    protected createMethod:
        PortfolioCreationMethod =
        'BY_AMOUNT';

    protected createInitialValue:
        number | null = null;

    protected createInitialCash:
        number | string | null = 0;

    protected holdingSearchQuery = '';

    protected holdingSearchResults:
        MarketSymbolSearchResponse[] = [];

    protected isSearchingHoldings = false;

    protected holdingSearchError = '';

    protected selectedMarketAsset:
        MarketSymbolSearchResponse | null =
        null;

    protected holdingQuantity:
        number | string | null = null;

    protected holdingDrafts:
        HoldingDraft[] = [];

    protected editName = '';

    protected editAssets:
        AssetResponse[] = [];

    protected editHoldings:
        EditHolding[] = [];

    protected editAvailableCash = 0;

    protected isLoadingEditPortfolio = false;

    protected editSearchQuery = '';

    protected editSearchResults:
        MarketSymbolSearchResponse[] = [];

    protected editSearchError = '';

    protected isSearchingEditAssets = false;

    protected editSelectedMarketAsset:
        MarketSymbolSearchResponse | null =
        null;

    protected editNewAssetQuantity:
        number | string | null = null;

    protected editNewAssetAmount:
        number | string | null = null;

    protected editAssetInputMode:
        EditAssetInputMode =
        'BY_QUANTITY';

    protected editSelectedAssetPrice:
        number | null = null;

    protected isLoadingEditAssetPrice =
        false;

    protected isAddingEditAsset = false;

    protected expandedPortfolioId:
        string | null = null;

    private readonly allocations =
        new Map<
            string,
            PortfolioAllocationResponse
        >();

    private readonly allocationLoadingIds =
        new Set<string>();

    private readonly allocationErrors =
        new Map<string, string>();

    ngOnInit(): void {
        this.subscribeToPortfolioChanges();
        this.loadPortfolios();
    }

    protected retry(): void {
        this.loadPortfolios();
    }

    protected goToPreviousPage(): void {
        if (
            this.isFirstPage ||
            this.isLoading
        ) {
            return;
        }

        this.currentPage -= 1;
        this.loadPortfolios();
    }

    protected goToNextPage(): void {
        if (
            this.isLastPage ||
            this.isLoading
        ) {
            return;
        }

        this.currentPage += 1;
        this.loadPortfolios();
    }

    protected togglePortfolioDetails(
        portfolio: PortfolioListItem,
    ): void {
        if (
            this.expandedPortfolioId ===
            portfolio.id
        ) {
            this.expandedPortfolioId =
                null;

            this.changeDetectorRef
                .markForCheck();

            return;
        }

        this.expandedPortfolioId =
            portfolio.id;

        if (
            !this.allocations.has(
                portfolio.id,
            ) &&
            !this.allocationLoadingIds.has(
                portfolio.id,
            )
        ) {
            this.loadPortfolioAllocation(
                portfolio.id,
            );
        }

        this.changeDetectorRef
            .markForCheck();
    }

    protected retryPortfolioAllocation(
        portfolioId: string,
    ): void {
        this.allocationErrors.delete(
            portfolioId,
        );

        this.allocations.delete(
            portfolioId,
        );

        this.loadPortfolioAllocation(
            portfolioId,
        );
    }

    protected isPortfolioExpanded(
        portfolioId: string,
    ): boolean {
        return (
            this.expandedPortfolioId ===
            portfolioId
        );
    }

    protected isAllocationLoading(
        portfolioId: string,
    ): boolean {
        return this.allocationLoadingIds
            .has(portfolioId);
    }

    protected getPortfolioAllocation(
        portfolioId: string,
    ): PortfolioAllocationResponse | null {
        return (
            this.allocations.get(
                portfolioId,
            ) ??
            null
        );
    }

    protected getAllocationError(
        portfolioId: string,
    ): string {
        return (
            this.allocationErrors.get(
                portfolioId,
            ) ??
            ''
        );
    }

    protected openCreateDialog(): void {
        this.clearMessages();

        this.selectedPortfolio = null;
        this.createName = '';
        this.createMethod = 'BY_AMOUNT';
        this.createInitialValue = null;
        this.createInitialCash = 0;

        this.resetHoldingCreationState();
        this.resetEditState();

        this.dialogMode = 'create';
    }

    protected onCreateMethodChange(): void {
        this.actionErrorMessage = '';

        if (
            this.createMethod ===
            'BY_AMOUNT'
        ) {
            this.resetHoldingCreationState();
            this.createInitialCash = 0;
        } else {
            this.createInitialValue = null;
            this.createInitialCash = 0;
        }

        this.changeDetectorRef
            .markForCheck();
    }

    protected onHoldingSearchQueryChange():
        void {
        this.holdingSearchError = '';
        this.actionErrorMessage = '';

        this.changeDetectorRef
            .markForCheck();
    }

    protected onHoldingQuantityChange():
        void {
        this.holdingSearchError = '';
        this.actionErrorMessage = '';

        this.changeDetectorRef
            .markForCheck();
    }

    protected onInitialCashChange():
        void {
        this.actionErrorMessage = '';

        this.changeDetectorRef
            .markForCheck();
    }

    protected searchHoldingAssets(): void {
        const query =
            this.holdingSearchQuery.trim();

        this.holdingSearchError = '';
        this.actionErrorMessage = '';

        this.selectedMarketAsset = null;

        if (query.length < 2) {
            this.holdingSearchResults = [];

            this.holdingSearchError =
                this.text()
                    .assets
                    .enterAtLeastTwoCharacters;

            this.changeDetectorRef
                .markForCheck();

            return;
        }

        this.isSearchingHoldings = true;
        this.holdingSearchResults = [];

        this.changeDetectorRef
            .markForCheck();

        this.marketDataService
            .searchSymbols({
                query,
                limit: 15,
            })
            .pipe(
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: (
                    results,
                ) => {
                    this.holdingSearchResults =
                        results;

                    this.isSearchingHoldings =
                        false;

                    if (
                        results.length === 0
                    ) {
                        this.holdingSearchError =
                            this.text()
                                .assets
                                .noMarketMatches;
                    }

                    this.changeDetectorRef
                        .markForCheck();
                },

                error: (
                    error: unknown,
                ) => {
                    this.isSearchingHoldings =
                        false;

                    this.holdingSearchResults = [];

                    this.holdingSearchError =
                        this.resolveMarketSearchError(
                            error,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    protected selectHoldingAsset(
        asset:
            MarketSymbolSearchResponse,
    ): void {
        this.selectedMarketAsset =
            asset;

        this.holdingQuantity = null;

        this.holdingSearchError = '';
        this.actionErrorMessage = '';

        this.changeDetectorRef
            .markForCheck();
    }

    protected clearSelectedHoldingAsset():
        void {
        this.selectedMarketAsset = null;
        this.holdingQuantity = null;
        this.holdingSearchError = '';

        this.changeDetectorRef
            .markForCheck();
    }

    protected addHoldingDraft(): void {
        const asset =
            this.selectedMarketAsset;

        if (!asset) {
            this.holdingSearchError =
                this.text()
                    .transactions
                    .selectAssetRequired;

            this.changeDetectorRef
                .markForCheck();

            return;
        }

        const normalizedQuantity =
            Number(
                this.holdingQuantity,
            );

        if (
            !Number.isFinite(
                normalizedQuantity,
            ) ||
            normalizedQuantity <= 0
        ) {
            this.holdingSearchError =
                this.text()
                    .transactions
                    .quantityInvalid;

            this.changeDetectorRef
                .markForCheck();

            return;
        }

        const duplicate =
            this.holdingDrafts.some(
                (holding) =>
                    this.isSameMarketAsset(
                        holding,
                        asset,
                    ),
            );

        if (duplicate) {
            this.holdingSearchError =
                this.text()
                    .assets
                    .duplicateSymbol;

            this.changeDetectorRef
                .markForCheck();

            return;
        }

        this.holdingDrafts = [
            ...this.holdingDrafts,
            {
                symbol:
                    asset.symbol,

                displayName:
                    asset.displayName,

                assetType:
                    asset.assetType,

                currency:
                    asset.currency,

                exchange:
                    asset.exchange,

                quantity:
                    normalizedQuantity,
            },
        ];

        this.selectedMarketAsset = null;
        this.holdingQuantity = null;
        this.holdingSearchQuery = '';
        this.holdingSearchResults = [];

        this.holdingSearchError = '';
        this.actionErrorMessage = '';

        this.changeDetectorRef
            .markForCheck();
    }

    protected removeHoldingDraft(
        index: number,
    ): void {
        this.holdingDrafts =
            this.holdingDrafts.filter(
                (
                    _holding,
                    holdingIndex,
                ) =>
                    holdingIndex !==
                    index,
            );

        this.actionErrorMessage = '';
        this.holdingSearchError = '';

        this.changeDetectorRef
            .markForCheck();
    }

    protected openPortfolioHistory(
        portfolioId: string,
    ): void {
        void this.router.navigate(
            [
                '/portfolios',
                portfolioId,
                'history',
            ],
        );
    }

    protected openEditDialog(
        portfolio: PortfolioListItem,
    ): void {
        this.clearMessages();

        this.selectedPortfolio =
            portfolio;

        this.editName =
            portfolio.name;

        this.resetEditState();

        this.dialogMode =
            'edit';

        this.loadEditPortfolioData(
            portfolio.id,
        );
    }

    protected openDeleteDialog(
        portfolio: PortfolioListItem,
    ): void {
        this.clearMessages();

        this.selectedPortfolio =
            portfolio;

        this.dialogMode =
            'delete';
    }

    protected closeDialog(): void {
        if (this.isSubmitting) {
            return;
        }

        this.dialogMode = null;
        this.selectedPortfolio = null;
        this.actionErrorMessage = '';

        this.resetHoldingCreationState();
        this.resetEditState();
    }

    protected submitCreate(): void {
        const normalizedName =
            this.createName.trim();

        if (!normalizedName) {
            this.actionErrorMessage =
                this.text()
                    .portfolios
                    .portfolioNameRequired;

            return;
        }

        if (
            this.createMethod ===
            'BY_HOLDINGS'
        ) {
            this.submitCreateFromHoldings(
                normalizedName,
            );

            return;
        }

        if (
            this.createInitialValue ===
            null ||
            !Number.isFinite(
                this.createInitialValue,
            ) ||
            this.createInitialValue <= 0
        ) {
            this.actionErrorMessage =
                this.text()
                    .portfolios
                    .initialValueInvalid;

            return;
        }

        const request:
            PortfolioCreateRequest = {
            name:
                normalizedName,
            creationMethod:
                'BY_AMOUNT',
            initialValue:
                this.createInitialValue,
        };

        this.startSubmission();

        this.portfolioService
            .createPortfolio(request)
            .subscribe({
                next: (
                    createdPortfolio,
                ) => {
                    this.handleCreatedPortfolio(
                        createdPortfolio,
                    );
                },

                error: (
                    error: unknown,
                ) => {
                    this.finishFailedSubmission(
                        this.resolveActionError(
                            error,
                            'create',
                        ),
                    );
                },
            });
    }

    protected setEditAssetInputMode(
        mode: EditAssetInputMode,
    ): void {
        this.editAssetInputMode =
            mode;

        this.editSearchError = '';

        this.changeDetectorRef
            .markForCheck();
    }

    protected get editDialogTitleLabel():
        string {
        return (
            this.text()
                .portfolios
                .cancel ===
            'ביטול'
        )
            ? 'עריכת תיק'
            : 'Edit portfolio';
    }

    protected get finishEditLabel():
        string {
        return (
            this.text()
                .portfolios
                .cancel ===
            'ביטול'
        )
            ? 'אישור וסיום'
            : 'Confirm & finish';
    }

    protected get removeHoldingLabel():
        string {
        return (
            this.text()
                .portfolios
                .cancel ===
            'ביטול'
        )
            ? 'הוצאה מהתיק'
            : 'Remove from portfolio';
    }

    protected get isEditOperationInProgress():
        boolean {
        return (
            this.isSubmitting ||
            this.isAddingEditAsset ||
            this.editHoldings.some(
                (holding) =>
                    holding.isSubmitting,
            )
        );
    }

    protected get calculatedEditAssetQuantity():
        number | null {
        if (
            this.editAssetInputMode !==
            'BY_AMOUNT' ||
            this.editSelectedAssetPrice ===
            null ||
            this.editSelectedAssetPrice <= 0
        ) {
            return null;
        }

        const amount =
            Number(
                this.editNewAssetAmount,
            );

        if (
            !Number.isFinite(amount) ||
            amount <= 0
        ) {
            return null;
        }

        return this.floorToEight(
            amount /
            this.editSelectedAssetPrice,
        );
    }

    protected finishEdit(): void {
        const portfolioId =
            this.selectedPortfolio?.id ??
            null;

        this.closeDialog();

        if (portfolioId) {
            this.portfolioService
                .notifyPortfolioChanged({
                    portfolioId,
                    reason:
                        'rebuilt',
                });
        }

        this.loadPortfolios();
    }

    protected saveEditName(): void {
        const portfolio =
            this.selectedPortfolio;

        const normalizedName =
            this.editName.trim();

        if (!portfolio) {
            return;
        }

        if (!normalizedName) {
            this.actionErrorMessage =
                this.text()
                    .portfolios
                    .portfolioNameRequired;

            return;
        }

        if (
            normalizedName ===
            portfolio.name
        ) {
            return;
        }

        this.startSubmission();

        this.portfolioService
            .renamePortfolio(
                portfolio.id,
                {
                    name:
                        normalizedName,
                },
            )
            .subscribe({
                next: () => {
                    portfolio.name =
                        normalizedName;

                    this.editName =
                        normalizedName;

                    this.portfolioService
                        .notifyPortfolioChanged({
                            portfolioId:
                                portfolio.id,
                            reason:
                                'renamed',
                        });

                    this.isSubmitting = false;

                    this.successMessage =
                        this.text()
                            .portfolios
                            .renamedSuccess;

                    this.changeDetectorRef
                        .markForCheck();
                },

                error: (
                    error: unknown,
                ) => {
                    this.finishFailedSubmission(
                        this.resolveActionError(
                            error,
                            'rename',
                        ),
                    );
                },
            });
    }

    protected updateEditHolding(
        holding: EditHolding,
    ): void {
        const portfolio =
            this.selectedPortfolio;

        if (
            !portfolio ||
            holding.isSubmitting
        ) {
            return;
        }

        const desiredQuantity =
            Number(
                holding.desiredQuantity,
            );

        if (
            !Number.isFinite(
                desiredQuantity,
            ) ||
            desiredQuantity < 0
        ) {
            holding.errorMessage =
                this.text()
                    .transactions
                    .quantityInvalid;

            this.changeDetectorRef
                .markForCheck();

            return;
        }

        const normalizedDesiredQuantity =
            this.roundToEight(
                desiredQuantity,
            );

        const difference =
            this.roundToEight(
                normalizedDesiredQuantity -
                holding.quantity,
            );

        if (
            Math.abs(difference) <
            0.000000005
        ) {
            holding.errorMessage = '';

            return;
        }

        holding.isSubmitting = true;
        holding.errorMessage = '';

        this.changeDetectorRef
            .markForCheck();

        this.marketDataService
            .getLatestPrice({
                symbol:
                    holding.symbol,
                exchange:
                    this.findEditAsset(
                        holding.assetId,
                    )?.exchange ?? null,
            })
            .pipe(
                switchMap(
                    (marketPrice) => {
                        const quantity =
                            this.roundToEight(
                                Math.abs(
                                    difference,
                                ),
                            );

                        const totalAmount =
                            this.roundToEight(
                                quantity *
                                marketPrice.price,
                            );

                        const request:
                            TransactionCreateRequest =
                        {
                            assetId:
                                holding.assetId,

                            transactionType:
                                difference > 0
                                    ? 'BUY'
                                    : 'SELL',

                            quantity,

                            unitPrice:
                                this.roundToEight(
                                    marketPrice.price,
                                ),

                            fee:
                                0,

                            totalAmount,

                            currency:
                                holding.currency,

                            executedAt:
                                new Date()
                                    .toISOString(),

                            notes:
                                normalizedDesiredQuantity ===
                                    0
                                    ? (
                                        'Portfolio edit: close holding'
                                    )
                                    : (
                                        'Portfolio edit quantity adjustment'
                                    ),
                        };

                        return this.transactionService
                            .createTransaction(
                                portfolio.id,
                                request,
                            );
                    },
                ),
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: () => {
                    holding.isSubmitting =
                        false;

                    this.successMessage =
                        this.text()
                            .transactions
                            .updatedSuccess;

                    this.loadEditPortfolioData(
                        portfolio.id,
                    );

                    this.changeDetectorRef
                        .markForCheck();
                },

                error: (
                    error: unknown,
                ) => {
                    holding.isSubmitting =
                        false;

                    holding.errorMessage =
                        this.resolveEditTransactionError(
                            error,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    protected removeEditHolding(
        holding: EditHolding,
    ): void {
        holding.desiredQuantity = 0;

        this.updateEditHolding(
            holding,
        );
    }

    protected searchEditAssets(): void {
        const query =
            this.editSearchQuery.trim();

        this.editSearchError = '';
        this.editSelectedMarketAsset =
            null;

        if (query.length < 2) {
            this.editSearchResults = [];

            this.editSearchError =
                this.text()
                    .assets
                    .enterAtLeastTwoCharacters;

            this.changeDetectorRef
                .markForCheck();

            return;
        }

        this.isSearchingEditAssets =
            true;

        this.editSearchResults = [];

        this.changeDetectorRef
            .markForCheck();

        this.marketDataService
            .searchSymbols({
                query,
                limit: 15,
            })
            .pipe(
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: (results) => {
                    this.editSearchResults =
                        results;

                    this.isSearchingEditAssets =
                        false;

                    if (
                        results.length === 0
                    ) {
                        this.editSearchError =
                            this.text()
                                .assets
                                .noMarketMatches;
                    }

                    this.changeDetectorRef
                        .markForCheck();
                },

                error: (
                    error: unknown,
                ) => {
                    this.isSearchingEditAssets =
                        false;

                    this.editSearchError =
                        this.resolveMarketSearchError(
                            error,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    protected selectEditAsset(
        asset:
            MarketSymbolSearchResponse,
    ): void {
        this.editSelectedMarketAsset =
            asset;

        this.editNewAssetQuantity =
            null;

        this.editNewAssetAmount =
            null;

        this.editSelectedAssetPrice =
            null;

        this.editSearchError = '';

        this.isLoadingEditAssetPrice =
            true;

        this.changeDetectorRef
            .markForCheck();

        this.marketDataService
            .getLatestPrice({
                symbol:
                    asset.symbol,
                exchange:
                    asset.exchange,
            })
            .pipe(
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: (response) => {
                    this.editSelectedAssetPrice =
                        response.price;

                    this.isLoadingEditAssetPrice =
                        false;

                    this.changeDetectorRef
                        .markForCheck();
                },

                error: (
                    error: unknown,
                ) => {
                    this.isLoadingEditAssetPrice =
                        false;

                    this.editSearchError =
                        this.resolveMarketSearchError(
                            error,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    protected clearEditSelectedAsset():
        void {
        this.editSelectedMarketAsset =
            null;

        this.editNewAssetQuantity =
            null;

        this.editNewAssetAmount =
            null;

        this.editSelectedAssetPrice =
            null;

        this.isLoadingEditAssetPrice =
            false;

        this.editSearchError = '';

        this.changeDetectorRef
            .markForCheck();
    }

    protected addEditAsset(): void {
        const portfolio =
            this.selectedPortfolio;

        const marketAsset =
            this.editSelectedMarketAsset;

        if (
            !portfolio ||
            !marketAsset
        ) {
            return;
        }

        const marketPrice =
            this.editSelectedAssetPrice;

        if (
            marketPrice === null ||
            marketPrice <= 0
        ) {
            this.editSearchError =
                this.text()
                    .assets
                    .marketPriceError;

            this.changeDetectorRef
                .markForCheck();

            return;
        }

        let quantity: number;
        let totalAmount: number;

        if (
            this.editAssetInputMode ===
            'BY_AMOUNT'
        ) {
            const requestedAmount =
                Number(
                    this.editNewAssetAmount,
                );

            if (
                !Number.isFinite(
                    requestedAmount,
                ) ||
                requestedAmount <= 0
            ) {
                this.editSearchError =
                    this.text()
                        .transactions
                        .totalAmountInvalid;

                this.changeDetectorRef
                    .markForCheck();

                return;
            }

            quantity =
                this.floorToEight(
                    requestedAmount /
                    marketPrice,
                );

            if (quantity <= 0) {
                this.editSearchError =
                    this.text()
                        .transactions
                        .quantityInvalid;

                this.changeDetectorRef
                    .markForCheck();

                return;
            }

            totalAmount =
                this.roundToEight(
                    quantity *
                    marketPrice,
                );
        } else {
            const requestedQuantity =
                Number(
                    this.editNewAssetQuantity,
                );

            if (
                !Number.isFinite(
                    requestedQuantity,
                ) ||
                requestedQuantity <= 0
            ) {
                this.editSearchError =
                    this.text()
                        .transactions
                        .quantityInvalid;

                this.changeDetectorRef
                    .markForCheck();

                return;
            }

            quantity =
                this.roundToEight(
                    requestedQuantity,
                );

            totalAmount =
                this.roundToEight(
                    quantity *
                    marketPrice,
                );
        }

        if (
            totalAmount >
            this.editAvailableCash +
            0.00000001
        ) {
            this.editSearchError =
                (
                    'Portfolio cash balance must not be negative. ' +
                    `Available: $${this.editAvailableCash.toFixed(2)}, ` +
                    `required: $${totalAmount.toFixed(2)}`
                );

            this.changeDetectorRef
                .markForCheck();

            return;
        }

        const existingAsset =
            this.findMatchingEditAsset(
                marketAsset,
            );

        this.isAddingEditAsset =
            true;

        this.editSearchError = '';

        this.changeDetectorRef
            .markForCheck();

        if (existingAsset) {
            this.createEditBuyTransaction(
                portfolio.id,
                existingAsset,
                quantity,
                marketPrice,
            )
                .pipe(
                    takeUntilDestroyed(
                        this.destroyRef,
                    ),
                )
                .subscribe({
                    next: () => {
                        this.finishSuccessfulEditAssetAdd(
                            portfolio.id,
                        );
                    },

                    error: (
                        error: unknown,
                    ) => {
                        this.isAddingEditAsset =
                            false;

                        this.editSearchError =
                            this.resolveEditTransactionError(
                                error,
                            );

                        this.changeDetectorRef
                            .markForCheck();
                    },
                });

            return;
        }

        const createRequest:
            AssetCreateRequest =
        {
            symbol:
                marketAsset.symbol,

            displayName:
                marketAsset.displayName,

            assetType:
                marketAsset.assetType,

            currency:
                marketAsset.currency,

            isin:
                null,

            exchange:
                marketAsset.exchange,

            notes:
                (
                    'Added from portfolio edit using live market search.'
                ),
        };

        this.assetService
            .createAsset(
                portfolio.id,
                createRequest,
            )
            .pipe(
                switchMap(
                    (
                        createdAsset,
                    ) =>
                        this.createEditBuyTransaction(
                            portfolio.id,
                            createdAsset,
                            quantity,
                            marketPrice,
                        ),
                ),
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: () => {
                    this.finishSuccessfulEditAssetAdd(
                        portfolio.id,
                    );
                },

                error: (
                    error: unknown,
                ) => {
                    this.isAddingEditAsset =
                        false;

                    this.editSearchError =
                        this.resolveEditTransactionError(
                            error,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    protected submitDelete(): void {
        const portfolio =
            this.selectedPortfolio;

        if (!portfolio) {
            return;
        }

        this.startSubmission();

        this.portfolioService
            .deletePortfolio(
                portfolio.id,
            )
            .subscribe({
                next: () => {
                    const
                        shouldMoveToPreviousPage =
                            (
                                this.portfolios
                                    .length === 1 &&
                                this.currentPage > 0
                            );

                    this.portfolioService
                        .notifyPortfolioChanged({
                            portfolioId:
                                portfolio.id,
                            reason:
                                'deleted',
                        });

                    this.finishSuccessfulSubmission(
                        this.text()
                            .portfolios
                            .deletedSuccess,
                    );

                    if (
                        shouldMoveToPreviousPage
                    ) {
                        this.currentPage -= 1;
                    }

                    this.loadPortfolios(
                        false,
                    );
                },

                error: (
                    error: unknown,
                ) => {
                    this.finishFailedSubmission(
                        this.resolveActionError(
                            error,
                            'delete',
                        ),
                    );
                },
            });
    }

    protected dismissSuccessMessage():
        void {
        this.successMessage = '';
    }

    protected isPositiveOrZero(
        value: number,
    ): boolean {
        return value >= 0;
    }

    protected get visiblePageNumber():
        number {
        if (
            this.totalPages === 0
        ) {
            return 0;
        }

        return (
            this.currentPage + 1
        );
    }

    protected get analyticsUnavailableCount():
        number {
        return this.portfolios.filter(
            (portfolio) =>
                !portfolio
                    .analyticsAvailable,
        ).length;
    }

    protected get totalCurrentValue():
        number {
        return this.portfolios.reduce(
            (
                total,
                portfolio,
            ) =>
                total +
                portfolio.currentValue,
            0,
        );
    }

    protected get displayedProfit():
        number {
        return this.portfolios.reduce(
            (
                total,
                portfolio,
            ) =>
                total +
                portfolio.totalProfit,
            0,
        );
    }

    private submitCreateFromHoldings(
        normalizedName: string,
    ): void {
        const normalizedInitialCash =
            Number(
                this.createInitialCash ?? 0,
            );

        if (
            !Number.isFinite(
                normalizedInitialCash,
            ) ||
            normalizedInitialCash < 0
        ) {
            this.actionErrorMessage =
                this.text()
                    .portfolios
                    .initialValueInvalid;

            return;
        }

        if (
            this.holdingDrafts.length ===
            0 &&
            normalizedInitialCash <= 0
        ) {
            this.actionErrorMessage =
                this.text()
                    .assets
                    .addFirstAsset;

            return;
        }

        const holdings:
            PortfolioHoldingCreateItemRequest[] =
            this.holdingDrafts.map(
                (holding) => ({
                    symbol:
                        holding.symbol,

                    displayName:
                        holding.displayName,

                    assetType:
                        holding.assetType,

                    currency:
                        holding.currency,

                    exchange:
                        holding.exchange,

                    quantity:
                        holding.quantity,
                }),
            );

        const request:
            PortfolioCreateFromHoldingsRequest =
        {
            name:
                normalizedName,
            holdings,
            initialCash:
                normalizedInitialCash,
        };

        this.startSubmission();

        this.portfolioService
            .createPortfolioFromHoldings(
                request,
            )
            .subscribe({
                next: (
                    createdPortfolio,
                ) => {
                    this.handleCreatedPortfolio(
                        createdPortfolio,
                    );
                },

                error: (
                    error: unknown,
                ) => {
                    this.finishFailedSubmission(
                        this.resolveActionError(
                            error,
                            'create',
                        ),
                    );
                },
            });
    }

    private handleCreatedPortfolio(
        createdPortfolio:
            PortfolioResponse,
    ): void {
        this.portfolioService
            .notifyPortfolioChanged({
                portfolioId:
                    createdPortfolio.id,
                reason:
                    'created',
            });

        this.finishSuccessfulSubmission(
            this.text()
                .portfolios
                .createdSuccess,
        );

        this.resetHoldingCreationState();

        this.currentPage = 0;

        this.loadPortfolios(
            false,
        );
    }

    private resetHoldingCreationState():
        void {
        this.holdingSearchQuery = '';
        this.holdingSearchResults = [];
        this.isSearchingHoldings = false;
        this.holdingSearchError = '';
        this.selectedMarketAsset = null;
        this.holdingQuantity = null;
        this.holdingDrafts = [];
        this.createInitialCash = 0;
    }

    private isSameMarketAsset(
        holding: HoldingDraft,
        asset:
            MarketSymbolSearchResponse,
    ): boolean {
        const holdingExchange =
            holding.exchange
                ?.trim()
                .toUpperCase() ??
            '';

        const assetExchange =
            asset.exchange
                ?.trim()
                .toUpperCase() ??
            '';

        return (
            holding.symbol
                .trim()
                .toUpperCase() ===
            asset.symbol
                .trim()
                .toUpperCase() &&
            holdingExchange ===
            assetExchange
        );
    }

    private resolveMarketSearchError(
        error: unknown,
    ): string {
        const portfolioTranslations =
            this.text().portfolios;

        const assetTranslations =
            this.text().assets;

        if (
            !(error instanceof
                HttpErrorResponse)
        ) {
            return assetTranslations
                .marketSearchError;
        }

        if (error.status === 0) {
            return portfolioTranslations
                .serverUnavailable;
        }

        if (error.status === 401) {
            return portfolioTranslations
                .sessionExpired;
        }

        if (error.status === 429) {
            return assetTranslations
                .marketRateLimit;
        }

        return (
            this.extractBackendMessage(
                error,
            ) ??
            assetTranslations
                .marketSearchError
        );
    }

    private loadEditPortfolioData(
        portfolioId: string,
    ): void {
        this.isLoadingEditPortfolio =
            true;

        this.actionErrorMessage = '';

        this.changeDetectorRef
            .markForCheck();

        forkJoin({
            allocation:
                this.portfolioService
                    .getPortfolioAllocation(
                        portfolioId,
                    ),

            assets:
                this.assetService
                    .getAssets(
                        portfolioId,
                    ),
        })
            .pipe(
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: ({
                    allocation,
                    assets,
                }) => {
                    this.editAssets =
                        assets;

                    this.editAvailableCash =
                        allocation
                            .cashBalance ??
                        0;

                    this.editHoldings =
                        allocation.assets
                            .map(
                                (asset) => ({
                                    assetId:
                                        asset.assetId,

                                    symbol:
                                        asset.symbol,

                                    displayName:
                                        asset.displayName,

                                    assetType:
                                        asset.assetType,

                                    currency:
                                        asset.currency,

                                    quantity:
                                        asset.quantity,

                                    desiredQuantity:
                                        asset.quantity,

                                    currentPrice:
                                        asset.currentPrice,

                                    isSubmitting:
                                        false,

                                    errorMessage:
                                        '',
                                }),
                            );

                    this.isLoadingEditPortfolio =
                        false;

                    this.changeDetectorRef
                        .markForCheck();
                },

                error: (
                    error: unknown,
                ) => {
                    this.isLoadingEditPortfolio =
                        false;

                    this.actionErrorMessage =
                        this.resolveAllocationError(
                            error,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    private findEditAsset(
        assetId: string,
    ): AssetResponse | null {
        return (
            this.editAssets.find(
                (asset) =>
                    asset.id ===
                    assetId,
            ) ??
            null
        );
    }

    private findMatchingEditAsset(
        marketAsset:
            MarketSymbolSearchResponse,
    ): AssetResponse | null {
        const symbol =
            marketAsset.symbol
                .trim()
                .toUpperCase();

        return (
            this.editAssets.find(
                (asset) =>
                    asset.symbol
                        .trim()
                        .toUpperCase() ===
                    symbol,
            ) ??
            null
        );
    }

    private createEditBuyTransaction(
        portfolioId: string,
        asset: AssetResponse,
        quantity: number,
        unitPrice: number,
    ) {
        const normalizedQuantity =
            this.roundToEight(
                quantity,
            );

        const normalizedUnitPrice =
            this.roundToEight(
                unitPrice,
            );

        const request:
            TransactionCreateRequest = {
            assetId:
                asset.id,

            transactionType:
                'BUY',

            quantity:
                normalizedQuantity,

            unitPrice:
                normalizedUnitPrice,

            fee:
                0,

            totalAmount:
                this.roundToEight(
                    normalizedQuantity *
                    normalizedUnitPrice,
                ),

            currency:
                asset.currency,

            executedAt:
                new Date()
                    .toISOString(),

            notes:
                (
                    'Asset added from portfolio edit'
                ),
        };

        return this.transactionService
            .createTransaction(
                portfolioId,
                request,
            );
    }

    private finishSuccessfulEditAssetAdd(
        portfolioId: string,
    ): void {
        this.isAddingEditAsset =
            false;

        this.editSearchQuery = '';
        this.editSearchResults = [];
        this.editSelectedMarketAsset =
            null;
        this.editNewAssetQuantity =
            null;
        this.editNewAssetAmount =
            null;
        this.editSelectedAssetPrice =
            null;

        this.successMessage =
            this.text()
                .transactions
                .createdSuccess;

        this.loadEditPortfolioData(
            portfolioId,
        );

        this.changeDetectorRef
            .markForCheck();
    }

    private resolveEditTransactionError(
        error: unknown,
    ): string {
        if (
            !(error instanceof
                HttpErrorResponse)
        ) {
            return this.text()
                .transactions
                .createError;
        }

        if (error.status === 0) {
            return this.text()
                .portfolios
                .serverUnavailable;
        }

        if (error.status === 401) {
            return this.text()
                .portfolios
                .sessionExpired;
        }

        if (error.status === 403) {
            return this.text()
                .portfolios
                .forbiddenAction;
        }

        return (
            this.extractBackendMessage(
                error,
            ) ??
            this.text()
                .transactions
                .createError
        );
    }

    private roundToEight(
        value: number,
    ): number {
        return Math.round(
            (
                value +
                Number.EPSILON
            ) *
            100_000_000,
        ) /
            100_000_000;
    }

    private floorToEight(
        value: number,
    ): number {
        return Math.floor(
            (
                value +
                Number.EPSILON
            ) *
            100_000_000,
        ) /
            100_000_000;
    }

    private resetEditState(): void {
        this.editAssets = [];
        this.editHoldings = [];
        this.editAvailableCash = 0;
        this.isLoadingEditPortfolio =
            false;
        this.editSearchQuery = '';
        this.editSearchResults = [];
        this.editSearchError = '';
        this.isSearchingEditAssets =
            false;
        this.editSelectedMarketAsset =
            null;
        this.editNewAssetQuantity =
            null;
        this.editNewAssetAmount =
            null;
        this.editAssetInputMode =
            'BY_QUANTITY';
        this.editSelectedAssetPrice =
            null;
        this.isLoadingEditAssetPrice =
            false;
        this.isAddingEditAsset =
            false;
    }

    private subscribeToPortfolioChanges():
        void {
        this.portfolioService
            .portfolioChanged$
            .pipe(
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe(
                (
                    event:
                        PortfolioChangeEvent,
                ) => {
                    this.refreshAfterPortfolioChange(
                        event,
                    );
                },
            );
    }

    private refreshAfterPortfolioChange(
        event: PortfolioChangeEvent,
    ): void {
        if (
            event.reason === 'created' ||
            event.reason === 'renamed' ||
            event.reason === 'deleted'
        ) {
            return;
        }

        const expandedPortfolioId =
            this.expandedPortfolioId;

        this.loadPortfolios(false);

        if (
            expandedPortfolioId &&
            (
                event.portfolioId ===
                null ||
                event.portfolioId ===
                expandedPortfolioId
            )
        ) {
            this.allocations.delete(
                expandedPortfolioId,
            );

            this.allocationErrors.delete(
                expandedPortfolioId,
            );
        }
    }

    private loadPortfolios(
        clearMessages = true,
    ): void {
        this.isLoading = true;
        this.errorMessage = '';

        this.resetPortfolioDetails();

        if (clearMessages) {
            this.successMessage = '';
        }

        this.changeDetectorRef
            .markForCheck();

        this.portfolioService
            .getPortfolios({
                page:
                    this.currentPage,
                size:
                    this.pageSize,
                sortBy:
                    'createdAt',
                sortDirection:
                    'desc',
            })
            .pipe(
                switchMap(
                    (response) =>
                        this.loadPortfolioSummaries(
                            response,
                        ),
                ),
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: (result) => {
                    this.applyResponse(
                        result.response,
                        result.portfolios,
                    );

                    this.isLoading =
                        false;

                    this.changeDetectorRef
                        .markForCheck();
                },

                error: (
                    error: unknown,
                ) => {
                    this.applyLoadFailure(
                        this.resolveLoadError(
                            error,
                        ),
                    );
                },
            });
    }

    private loadPortfolioSummaries(
        response:
            PagedResponse<PortfolioResponse>,
    ): Observable<PortfolioPageResult> {
        if (
            response.content.length ===
            0
        ) {
            return of({
                response,
                portfolios: [],
            });
        }

        const summaryRequests =
            response.content.map(
                (portfolio) =>
                    this.loadPortfolioSummary(
                        portfolio,
                    ),
            );

        return forkJoin(
            summaryRequests,
        ).pipe(
            map(
                (portfolios) => ({
                    response,
                    portfolios,
                }),
            ),
        );
    }

    private loadPortfolioSummary(
        portfolio: PortfolioResponse,
    ): Observable<PortfolioListItem> {
        return this.portfolioService
            .getPortfolioSummary(
                portfolio.id,
            )
            .pipe(
                map(
                    (summary) =>
                        this.createPortfolioListItem(
                            portfolio,
                            summary,
                        ),
                ),

                catchError(
                    () =>
                        of(
                            this.createFallbackPortfolio(
                                portfolio,
                            ),
                        ),
                ),
            );
    }

    private loadPortfolioAllocation(
        portfolioId: string,
    ): void {
        this.allocationLoadingIds
            .add(portfolioId);

        this.allocationErrors
            .delete(portfolioId);

        this.changeDetectorRef
            .markForCheck();

        this.portfolioService
            .getPortfolioAllocation(
                portfolioId,
            )
            .pipe(
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: (
                    allocation,
                ) => {
                    this.allocations.set(
                        portfolioId,
                        allocation,
                    );

                    this.allocationLoadingIds
                        .delete(
                            portfolioId,
                        );

                    this.allocationErrors
                        .delete(
                            portfolioId,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },

                error: (
                    error: unknown,
                ) => {
                    this.allocations.delete(
                        portfolioId,
                    );

                    this.allocationLoadingIds
                        .delete(
                            portfolioId,
                        );

                    this.allocationErrors.set(
                        portfolioId,
                        this.resolveAllocationError(
                            error,
                        ),
                    );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    private createPortfolioListItem(
        portfolio: PortfolioResponse,
        summary:
            PortfolioSummaryResponse,
    ): PortfolioListItem {
        return {
            id:
                portfolio.id,

            userId:
                portfolio.userId,

            name:
                summary.portfolioName,

            creationMethod:
                portfolio.creationMethod,

            initialValue:
                summary.initialValue,

            currentValue:
                summary.currentValue,

            totalRealizedProfit:
                summary
                    .totalRealizedProfit,

            totalUnrealizedProfit:
                summary
                    .totalUnrealizedProfit,

            totalProfit:
                summary.totalProfit,

            totalReturnPercent:
                summary
                    .totalReturnPercent,

            activeAssetCount:
                summary
                    .activeAssetCount,

            transactionCount:
                summary
                    .transactionCount,

            calculatedAt:
                summary.calculatedAt,

            createdAt:
                portfolio.createdAt,

            updatedAt:
                portfolio.updatedAt,

            analyticsAvailable:
                true,
        };
    }

    private createFallbackPortfolio(
        portfolio: PortfolioResponse,
    ): PortfolioListItem {
        return {
            id:
                portfolio.id,

            userId:
                portfolio.userId,

            name:
                portfolio.name,

            creationMethod:
                portfolio.creationMethod,

            initialValue:
                portfolio.initialValue,

            currentValue:
                portfolio.currentValue,

            totalRealizedProfit:
                portfolio
                    .totalRealizedProfit,

            totalUnrealizedProfit:
                portfolio
                    .totalUnrealizedProfit,

            totalProfit:
                (
                    portfolio.currentValue -
                    portfolio.initialValue
                ),

            totalReturnPercent:
                portfolio
                    .totalReturnPercent,

            activeAssetCount:
                0,

            transactionCount:
                0,

            calculatedAt:
                portfolio.updatedAt,

            createdAt:
                portfolio.createdAt,

            updatedAt:
                portfolio.updatedAt,

            analyticsAvailable:
                false,
        };
    }

    private applyResponse(
        response:
            PagedResponse<PortfolioResponse>,
        portfolios:
            PortfolioListItem[],
    ): void {
        this.portfolios =
            portfolios;

        this.currentPage =
            response.page;

        this.pageSize =
            response.size;

        this.totalElements =
            response.totalElements;

        this.totalPages =
            response.totalPages;

        this.isFirstPage =
            response.first;

        this.isLastPage =
            response.last;
    }

    private applyLoadFailure(
        message: string,
    ): void {
        this.isLoading = false;
        this.portfolios = [];
        this.totalElements = 0;
        this.totalPages = 0;
        this.isFirstPage = true;
        this.isLastPage = true;
        this.errorMessage = message;

        this.changeDetectorRef
            .markForCheck();
    }

    private resetPortfolioDetails():
        void {
        this.expandedPortfolioId =
            null;

        this.allocations.clear();

        this.allocationLoadingIds
            .clear();

        this.allocationErrors.clear();
    }

    private startSubmission(): void {
        this.isSubmitting = true;
        this.actionErrorMessage = '';

        this.changeDetectorRef
            .markForCheck();
    }

    private finishSuccessfulSubmission(
        message: string,
    ): void {
        this.isSubmitting = false;
        this.dialogMode = null;
        this.selectedPortfolio = null;
        this.successMessage = message;

        this.changeDetectorRef
            .markForCheck();
    }

    private finishFailedSubmission(
        message: string,
    ): void {
        this.isSubmitting = false;
        this.actionErrorMessage =
            message;

        this.changeDetectorRef
            .markForCheck();
    }

    private resolveLoadError(
        error: unknown,
    ): string {
        const translations =
            this.text().portfolios;

        if (
            !(error instanceof
                HttpErrorResponse)
        ) {
            return translations
                .loadError;
        }

        if (
            error.status === 0
        ) {
            return translations
                .serverUnavailable;
        }

        if (
            error.status === 401
        ) {
            return translations
                .sessionExpired;
        }

        if (
            error.status === 403
        ) {
            return translations
                .forbiddenView;
        }

        return translations
            .loadError;
    }

    private resolveAllocationError(
        error: unknown,
    ): string {
        const translations =
            this.text().portfolios;

        if (
            !(error instanceof
                HttpErrorResponse)
        ) {
            return translations
                .allocationLoadError;
        }

        if (
            error.status === 0
        ) {
            return translations
                .serverUnavailable;
        }

        if (
            error.status === 401
        ) {
            return translations
                .sessionExpired;
        }

        if (
            error.status === 403
        ) {
            return translations
                .allocationForbidden;
        }

        if (
            error.status === 404
        ) {
            return translations
                .allocationNotFound;
        }

        return (
            this.extractBackendMessage(
                error,
            ) ??
            translations
                .allocationLoadError
        );
    }

    private resolveActionError(
        error: unknown,
        action:
            PortfolioAction,
    ): string {
        const translations =
            this.text().portfolios;

        if (
            !(error instanceof
                HttpErrorResponse)
        ) {
            return this
                .defaultActionError(
                    action,
                );
        }

        if (
            error.status === 0
        ) {
            return translations
                .serverUnavailable;
        }

        if (
            error.status === 401
        ) {
            return translations
                .sessionExpired;
        }

        if (
            error.status === 403
        ) {
            return translations
                .forbiddenAction;
        }

        if (
            error.status === 409
        ) {
            if (
                action === 'create' ||
                action === 'rename'
            ) {
                return translations
                    .duplicateName;
            }

            return translations
                .portfolioConflict;
        }

        if (
            error.status === 400
        ) {
            return (
                this.extractBackendMessage(
                    error,
                ) ??
                translations
                    .invalidPortfolio
            );
        }

        return (
            this.extractBackendMessage(
                error,
            ) ??
            this.defaultActionError(
                action,
            )
        );
    }

    private extractBackendMessage(
        error: HttpErrorResponse,
    ): string | null {
        const responseBody =
            error.error;

        if (
            responseBody &&
            typeof responseBody ===
            'object'
        ) {
            const candidate =
                'message' in responseBody
                    ? responseBody.message
                    : 'detail' in responseBody
                        ? responseBody.detail
                        : null;

            if (
                typeof candidate ===
                'string' &&
                candidate.trim()
            ) {
                return candidate.trim();
            }
        }

        if (
            typeof responseBody ===
            'string' &&
            responseBody.trim()
        ) {
            return responseBody.trim();
        }

        return null;
    }

    private defaultActionError(
        action:
            PortfolioAction,
    ): string {
        const translations =
            this.text().portfolios;

        switch (action) {
            case 'create':
                return translations
                    .createError;

            case 'rename':
                return translations
                    .renameError;

            case 'delete':
                return translations
                    .deleteError;
        }
    }

    private clearMessages(): void {
        this.actionErrorMessage = '';
        this.successMessage = '';
    }
}