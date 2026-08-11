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
    AllocationPurchaseExecutionResponse,
    AllocationPurchasePreviewResponse,
} from '../../../../core/allocation-purchase/models/allocation-purchase.models';
import {
    AllocationPurchaseService,
} from '../../../../core/allocation-purchase/services/allocation-purchase.service';
import {
    AssetCreateRequest,
    AssetResponse,
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
    PortfolioResponse,
} from '../../../../core/portfolio/models/portfolio.models';
import {
    PortfolioService,
} from '../../../../core/portfolio/services/portfolio.service';

@Component({
    selector: 'app-allocation-purchase-page',
    imports: [
        CurrencyPipe,
        DatePipe,
        DecimalPipe,
        FormsModule,
    ],
    templateUrl:
        './allocation-purchase-page.html',
    styleUrl:
        './allocation-purchase-page.scss',
    changeDetection:
        ChangeDetectionStrategy.OnPush,
})
export class AllocationPurchasePage
    implements OnInit {

    private readonly allocationPurchaseService =
        inject(AllocationPurchaseService);

    private readonly portfolioService =
        inject(PortfolioService);

    private readonly assetService =
        inject(AssetService);

    private readonly marketDataService =
        inject(MarketDataService);

    private readonly translationService =
        inject(TranslationService);

    private readonly changeDetectorRef =
        inject(ChangeDetectorRef);

    private readonly destroyRef =
        inject(DestroyRef);

    protected readonly text =
        this.translationService.text;

    protected portfolios:
        PortfolioResponse[] = [];

    protected assets:
        AssetResponse[] = [];

    protected selectedPortfolioId = '';

    protected selectedAssetId = '';

    protected targetWeightPercent:
        number | null = null;

    protected fee:
        number | null = null;

    protected marketSearchQuery = '';

    protected marketSearchResults:
        MarketSymbolSearchResponse[] = [];

    protected selectedMarketResult:
        MarketSymbolSearchResponse | null =
        null;

    protected preview:
        AllocationPurchasePreviewResponse | null =
        null;

    protected execution:
        AllocationPurchaseExecutionResponse | null =
        null;

    protected isLoadingPortfolios = true;

    protected isLoadingAssets = false;

    protected isSearchingMarket = false;

    protected isAddingMarketAsset = false;

    protected isPreviewing = false;

    protected isExecuting = false;

    protected errorMessage = '';

    protected successMessage = '';

    protected marketSearchMessage = '';

    ngOnInit(): void {
        this.loadPortfolios();
    }

    protected selectPortfolio(
        portfolioId: string,
    ): void {
        if (
            this.selectedPortfolioId ===
            portfolioId
        ) {
            return;
        }

        this.selectedPortfolioId =
            portfolioId;

        this.resetAssetSelection();
        this.resetMarketSearch();

        if (!portfolioId) {
            this.assets = [];

            this.changeDetectorRef
                .markForCheck();

            return;
        }

        this.loadAssets(
            portfolioId,
        );
    }

    protected selectAsset(
        assetId: string,
    ): void {
        this.selectedAssetId =
            assetId;

        this.selectedMarketResult =
            null;

        this.clearPreviewAndExecution();
        this.clearMessages();

        this.changeDetectorRef
            .markForCheck();
    }

    protected onMarketSearchQueryChange(
        value: string,
    ): void {
        this.marketSearchQuery =
            value;

        this.marketSearchResults = [];
        this.selectedMarketResult = null;
        this.marketSearchMessage = '';

        this.changeDetectorRef
            .markForCheck();
    }

    protected searchMarket(): void {
        this.marketSearchMessage = '';
        this.selectedMarketResult = null;

        const query =
            this.marketSearchQuery.trim();

        if (query.length < 2) {
            this.marketSearchResults = [];

            this.marketSearchMessage =
                this.text()
                    .assets
                    .enterAtLeastTwoCharacters;

            this.changeDetectorRef
                .markForCheck();

            return;
        }

        this.isSearchingMarket = true;
        this.marketSearchResults = [];

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
                    this.marketSearchResults =
                        results;

                    this.isSearchingMarket =
                        false;

                    if (
                        results.length === 0
                    ) {
                        this.marketSearchMessage =
                            this.text()
                                .assets
                                .noMarketMatches;
                    }

                    this.changeDetectorRef
                        .markForCheck();
                },
                error: (error: unknown) => {
                    this.isSearchingMarket =
                        false;

                    this.marketSearchMessage =
                        this.resolveError(
                            error,
                            this.text()
                                .assets
                                .marketSearchError,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    protected selectMarketResult(
        result: MarketSymbolSearchResponse,
    ): void {
        this.selectedMarketResult =
            result;

        this.marketSearchMessage = '';

        const existingAsset =
            this.findExistingAsset(
                result,
            );

        if (existingAsset) {
            this.selectedAssetId =
                existingAsset.id;

            this.selectedMarketResult =
                null;

            this.marketSearchResults = [];

            this.marketSearchQuery =
                (
                    existingAsset.symbol
                    + ' · '
                    + existingAsset.displayName
                );

            this.clearPreviewAndExecution();
            this.clearMessages();

            this.changeDetectorRef
                .markForCheck();

            return;
        }

        this.selectedAssetId = '';

        this.clearPreviewAndExecution();
        this.clearMessages();

        this.changeDetectorRef
            .markForCheck();
    }

    protected clearMarketSelection(): void {
        this.resetMarketSearch();

        this.changeDetectorRef
            .markForCheck();
    }

    protected addSelectedMarketAsset(): void {
        this.clearMessages();

        if (
            !this.selectedPortfolioId ||
            !this.selectedMarketResult
        ) {
            return;
        }

        const existingAsset =
            this.findExistingAsset(
                this.selectedMarketResult,
            );

        if (existingAsset) {
            this.selectedAssetId =
                existingAsset.id;

            this.resetMarketSearch();

            this.changeDetectorRef
                .markForCheck();

            return;
        }

        const result =
            this.selectedMarketResult;

        const request:
            AssetCreateRequest = {
            symbol:
                result.symbol,
            displayName:
                result.displayName,
            assetType:
                result.assetType,
            currency:
                result.currency,
            isin:
                null,
            exchange:
                result.exchange,
            notes:
                (
                    'Added from the allocation screen ' +
                    'using live market search.'
                ),
        };

        this.isAddingMarketAsset = true;

        this.changeDetectorRef
            .markForCheck();

        this.assetService
            .createAsset(
                this.selectedPortfolioId,
                request,
            )
            .pipe(
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: (createdAsset) => {
                    this.assets = [
                        ...this.assets,
                        createdAsset,
                    ];

                    this.selectedAssetId =
                        createdAsset.id;

                    this.isAddingMarketAsset =
                        false;

                    this.successMessage =
                        this.text()
                            .assets
                            .createdSuccess;

                    this.resetMarketSearch();

                    this.clearPreviewAndExecution();

                    this.changeDetectorRef
                        .markForCheck();
                },
                error: (error: unknown) => {
                    this.isAddingMarketAsset =
                        false;

                    this.errorMessage =
                        this.resolveError(
                            error,
                            this.text()
                                .assets
                                .createError,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    protected requestPreview(): void {
        this.clearMessages();
        this.execution = null;

        if (!this.validateInput()) {
            return;
        }

        this.isPreviewing = true;

        this.changeDetectorRef
            .markForCheck();

        this.allocationPurchaseService
            .previewPurchase(
                this.selectedPortfolioId,
                {
                    assetId:
                        this.selectedAssetId,
                    targetWeightPercent:
                        this.targetWeightPercent!,
                },
            )
            .pipe(
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: (response) => {
                    this.preview = response;
                    this.isPreviewing = false;

                    this.changeDetectorRef
                        .markForCheck();
                },
                error: (error: unknown) => {
                    this.preview = null;
                    this.isPreviewing = false;

                    this.errorMessage =
                        this.resolveError(
                            error,
                            this.text()
                                .allocationPurchase
                                .previewError,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    protected executePurchase(): void {
        this.clearMessages();

        if (!this.preview) {
            this.errorMessage =
                this.text()
                    .allocationPurchase
                    .previewRequired;

            return;
        }

        if (
            this.preview.suggestedAction ===
            'HOLD'
        ) {
            this.errorMessage =
                this.text()
                    .allocationPurchase
                    .alreadyAtTargetMessage;

            return;
        }

        this.isExecuting = true;

        this.changeDetectorRef
            .markForCheck();

        this.allocationPurchaseService
            .executePurchase(
                this.selectedPortfolioId,
                {
                    assetId:
                        this.selectedAssetId,
                    targetWeightPercent:
                        this.targetWeightPercent!,
                    fee:
                        this.fee,
                },
            )
            .pipe(
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: (response) => {
                    this.execution = response;
                    this.preview = null;
                    this.isExecuting = false;

                    this.portfolioService
                        .notifyPortfolioChanged({
                            portfolioId:
                                this.selectedPortfolioId,
                            reason:
                                'allocation-executed',
                        });

                    const actionLabel =
                        response.action === 'SELL'
                            ? this.text()
                                .transactions
                                .typeSell
                            : this.text()
                                .transactions
                                .typeBuy;

                    this.successMessage =
                        (
                            `${actionLabel} ` +
                            `${response.tradedQuantity} ` +
                            `${response.symbol} ` +
                            `${this.text()
                                .allocationPurchase
                                .purchaseSuccessSuffix}`
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
                error: (error: unknown) => {
                    this.execution = null;
                    this.isExecuting = false;

                    this.errorMessage =
                        this.resolveError(
                            error,
                            this.text()
                                .allocationPurchase
                                .executeError,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    protected clearResult(): void {
        this.clearPreviewAndExecution();
        this.clearMessages();

        this.targetWeightPercent =
            null;

        this.fee =
            null;

        this.changeDetectorRef
            .markForCheck();
    }

    protected dismissSuccessMessage(): void {
        this.successMessage = '';

        this.changeDetectorRef
            .markForCheck();
    }

    protected retry(): void {
        this.loadPortfolios();
    }

    protected get selectedPortfolio():
        PortfolioResponse | null {
        return (
            this.portfolios.find(
                (portfolio) =>
                    portfolio.id ===
                    this.selectedPortfolioId,
            ) ?? null
        );
    }

    protected get selectedAsset():
        AssetResponse | null {
        return (
            this.assets.find(
                (asset) =>
                    asset.id ===
                    this.selectedAssetId,
            ) ?? null
        );
    }

    protected get canPreview(): boolean {
        return (
            Boolean(
                this.selectedPortfolioId,
            ) &&
            Boolean(
                this.selectedAssetId,
            ) &&
            this.targetWeightPercent !== null &&
            this.targetWeightPercent >= 0 &&
            this.targetWeightPercent <= 100 &&
            !this.isPreviewing &&
            !this.isExecuting &&
            !this.isAddingMarketAsset
        );
    }

    protected get canAddSelectedMarketAsset():
        boolean {
        return (
            Boolean(
                this.selectedPortfolioId,
            ) &&
            this.selectedMarketResult !== null &&
            !this.isAddingMarketAsset &&
            !this.isPreviewing &&
            !this.isExecuting
        );
    }

    protected get canExecute(): boolean {
        return (
            this.preview !== null &&
            (
                this.preview.suggestedAction ===
                'BUY' ||
                this.preview.suggestedAction ===
                'SELL'
            ) &&
            !this.isExecuting &&
            !this.isPreviewing
        );
    }

    private loadPortfolios(): void {
        this.isLoadingPortfolios = true;
        this.errorMessage = '';

        this.portfolios = [];
        this.assets = [];

        this.selectedPortfolioId = '';
        this.selectedAssetId = '';

        this.resetMarketSearch();
        this.clearPreviewAndExecution();

        this.changeDetectorRef
            .markForCheck();

        this.portfolioService
            .getPortfolios({
                page: 0,
                size: 100,
                sortBy: 'createdAt',
                sortDirection: 'desc',
            })
            .pipe(
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: (response) => {
                    this.portfolios =
                        response.content;

                    this.isLoadingPortfolios =
                        false;

                    if (
                        this.portfolios.length > 0
                    ) {
                        this.selectedPortfolioId =
                            this.portfolios[0].id;

                        this.loadAssets(
                            this.selectedPortfolioId,
                        );
                    }

                    this.changeDetectorRef
                        .markForCheck();
                },
                error: (error: unknown) => {
                    this.isLoadingPortfolios =
                        false;

                    this.errorMessage =
                        this.resolveError(
                            error,
                            this.text()
                                .allocationPurchase
                                .portfoliosLoadError,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    private loadAssets(
        portfolioId: string,
    ): void {
        this.isLoadingAssets = true;

        this.assets = [];
        this.selectedAssetId = '';

        this.clearPreviewAndExecution();
        this.clearMessages();

        this.changeDetectorRef
            .markForCheck();

        this.assetService
            .getAssets(
                portfolioId,
            )
            .pipe(
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: (assets) => {
                    this.assets =
                        assets;

                    this.isLoadingAssets =
                        false;

                    if (
                        this.assets.length > 0
                    ) {
                        this.selectedAssetId =
                            this.assets[0].id;
                    }

                    this.changeDetectorRef
                        .markForCheck();
                },
                error: (error: unknown) => {
                    this.isLoadingAssets =
                        false;

                    this.errorMessage =
                        this.resolveError(
                            error,
                            this.text()
                                .allocationPurchase
                                .assetsLoadError,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    private findExistingAsset(
        result: MarketSymbolSearchResponse,
    ): AssetResponse | null {
        const normalizedSymbol =
            result.symbol
                .trim()
                .toUpperCase();

        const normalizedExchange =
            result.exchange
                ?.trim()
                .toUpperCase() ?? '';

        return (
            this.assets.find(
                (asset) => {
                    const assetSymbol =
                        asset.symbol
                            .trim()
                            .toUpperCase();

                    const assetExchange =
                        asset.exchange
                            ?.trim()
                            .toUpperCase() ?? '';

                    return (
                        assetSymbol ===
                        normalizedSymbol &&
                        assetExchange ===
                        normalizedExchange
                    );
                },
            ) ?? null
        );
    }

    private validateInput(): boolean {
        if (!this.selectedPortfolioId) {
            this.errorMessage =
                this.text()
                    .allocationPurchase
                    .selectPortfolio;

            return false;
        }

        if (!this.selectedAssetId) {
            this.errorMessage =
                this.text()
                    .allocationPurchase
                    .selectAsset;

            return false;
        }

        if (
            this.targetWeightPercent ===
            null
        ) {
            this.errorMessage =
                this.text()
                    .allocationPurchase
                    .targetWeightRequired;

            return false;
        }

        if (
            this.targetWeightPercent < 0 ||
            this.targetWeightPercent > 100
        ) {
            this.errorMessage =
                this.text()
                    .allocationPurchase
                    .targetWeightInvalid;

            return false;
        }

        if (
            this.fee !== null &&
            this.fee < 0
        ) {
            this.errorMessage =
                this.text()
                    .allocationPurchase
                    .feeInvalid;

            return false;
        }

        return true;
    }

    private resetAssetSelection(): void {
        this.assets = [];
        this.selectedAssetId = '';
        this.targetWeightPercent = null;
        this.fee = null;

        this.clearPreviewAndExecution();
        this.clearMessages();
    }

    private resetMarketSearch(): void {
        this.marketSearchQuery = '';
        this.marketSearchResults = [];
        this.selectedMarketResult = null;
        this.marketSearchMessage = '';
        this.isSearchingMarket = false;
    }

    private clearPreviewAndExecution(): void {
        this.preview = null;
        this.execution = null;
    }

    private clearMessages(): void {
        this.errorMessage = '';
        this.successMessage = '';
    }

    private resolveError(
        error: unknown,
        fallbackMessage: string,
    ): string {
        const translations =
            this.text().allocationPurchase;

        if (
            !(error instanceof
                HttpErrorResponse)
        ) {
            return fallbackMessage;
        }

        if (error.status === 0) {
            return translations
                .serverUnavailable;
        }

        if (error.status === 401) {
            return translations
                .sessionExpired;
        }

        if (error.status === 403) {
            return translations
                .forbiddenAction;
        }

        if (error.status === 404) {
            return translations
                .resourceNotFound;
        }

        const backendMessage =
            this.extractBackendMessage(
                error,
            );

        return (
            backendMessage ??
            fallbackMessage
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
}