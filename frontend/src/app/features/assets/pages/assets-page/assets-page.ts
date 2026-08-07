import {
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
    Subject,
    catchError,
    debounceTime,
    distinctUntilChanged,
    finalize,
    map,
    of,
    switchMap,
    tap,
} from 'rxjs';

import {
    ASSET_TYPE_OPTIONS,
    AssetCreateRequest,
    AssetResponse,
    AssetType,
    AssetUpdateRequest,
} from '../../../../core/asset/models/asset.models';
import {
    AssetService,
} from '../../../../core/asset/services/asset.service';
import {
    TranslationService,
} from '../../../../core/i18n/services/translation.service';
import {
    MarketPriceResponse,
    MarketSymbolSearchResponse,
} from '../../../../core/market-data/models/market-data.models';
import {
    MarketDataService,
} from '../../../../core/market-data/services/market-data.service';
import {
    PagedResponse,
    PortfolioResponse,
} from '../../../../core/portfolio/models/portfolio.models';
import {
    PortfolioService,
} from '../../../../core/portfolio/services/portfolio.service';

type AssetDialogMode =
    | 'create'
    | 'edit'
    | 'delete'
    | null;

type AssetAction =
    | 'create'
    | 'edit'
    | 'delete';

interface AssetFormValue {
    symbol: string;
    displayName: string;
    assetType: AssetType;
    currency: string;
    isin: string;
    exchange: string;
    notes: string;
}

@Component({
    selector: 'app-assets-page',
    imports: [
        DatePipe,
        DecimalPipe,
        FormsModule,
    ],
    templateUrl: './assets-page.html',
    styleUrl: './assets-page.scss',
    changeDetection:
        ChangeDetectionStrategy.OnPush,
})
export class AssetsPage
    implements OnInit {

    private readonly assetService =
        inject(AssetService);

    private readonly marketDataService =
        inject(MarketDataService);

    private readonly portfolioService =
        inject(PortfolioService);

    private readonly translationService =
        inject(TranslationService);

    private readonly changeDetectorRef =
        inject(ChangeDetectorRef);

    private readonly destroyRef =
        inject(DestroyRef);

    private readonly marketSearchSubject =
        new Subject<string>();

    protected readonly text =
        this.translationService.text;

    protected readonly assetTypeOptions =
        ASSET_TYPE_OPTIONS;

    protected portfolios:
        PortfolioResponse[] = [];

    protected assets:
        AssetResponse[] = [];

    protected filteredAssets:
        AssetResponse[] = [];

    protected marketSearchResults:
        MarketSymbolSearchResponse[] = [];

    protected selectedPortfolioId = '';
    protected searchValue = '';
    protected marketSearchValue = '';

    protected isLoadingPortfolios = true;
    protected isLoadingAssets = false;
    protected isSubmitting = false;
    protected isSearchingMarket = false;
    protected isLoadingMarketPrice = false;

    protected errorMessage = '';
    protected actionErrorMessage = '';
    protected successMessage = '';
    protected marketSearchMessage = '';

    protected selectedMarketResult:
        MarketSymbolSearchResponse | null =
        null;

    protected selectedMarketPrice:
        MarketPriceResponse | null =
        null;

    protected dialogMode:
        AssetDialogMode = null;

    protected selectedAsset:
        AssetResponse | null = null;

    protected formValue:
        AssetFormValue =
        this.createEmptyFormValue();

    ngOnInit(): void {
        this.configureMarketSearch();
        this.loadPortfolios();
    }

    protected retry(): void {
        if (
            this.portfolios.length === 0
        ) {
            this.loadPortfolios();
            return;
        }

        this.loadAssets();
    }

    protected selectPortfolio(
        portfolioId: string,
    ): void {
        if (
            portfolioId ===
            this.selectedPortfolioId ||
            this.isLoadingAssets
        ) {
            return;
        }

        this.selectedPortfolioId =
            portfolioId;

        this.searchValue = '';

        this.loadAssets();
    }

    protected onSearchChange(): void {
        this.applySearchFilter();
    }

    protected onMarketSearchChange(
        value: string,
    ): void {
        this.marketSearchValue = value;
        this.selectedMarketResult = null;
        this.selectedMarketPrice = null;
        this.marketSearchMessage = '';

        this.marketSearchSubject.next(
            value,
        );
    }

    protected selectMarketResult(
        result:
            MarketSymbolSearchResponse,
    ): void {
        this.selectedMarketResult =
            result;

        this.formValue = {
            ...this.formValue,
            symbol:
                result.symbol,
            displayName:
                result.displayName,
            assetType:
                result.assetType,
            currency:
                result.currency,
            exchange:
                result.exchange ?? '',
        };

        this.marketSearchValue =
            `${result.symbol} · ${result.displayName}`;

        this.marketSearchResults = [];
        this.marketSearchMessage = '';
        this.selectedMarketPrice = null;

        this.loadLatestMarketPrice(
            result,
        );

        this.changeDetectorRef
            .markForCheck();
    }

    protected clearSelectedMarketResult():
        void {
        this.selectedMarketResult = null;
        this.selectedMarketPrice = null;
        this.marketSearchValue = '';
        this.marketSearchResults = [];
        this.marketSearchMessage = '';

        this.changeDetectorRef
            .markForCheck();
    }

    protected openCreateDialog():
        void {
        if (
            !this.selectedPortfolioId
        ) {
            return;
        }

        this.clearMessages();
        this.resetMarketSearchState();

        this.selectedAsset = null;

        this.formValue =
            this.createEmptyFormValue();

        this.dialogMode = 'create';

        this.changeDetectorRef
            .markForCheck();
    }

    protected openEditDialog(
        asset: AssetResponse,
    ): void {
        this.clearMessages();
        this.resetMarketSearchState();

        this.selectedAsset = asset;

        this.formValue = {
            symbol:
                asset.symbol,
            displayName:
                asset.displayName,
            assetType:
                asset.assetType,
            currency:
                asset.currency,
            isin:
                asset.isin ?? '',
            exchange:
                asset.exchange ?? '',
            notes:
                asset.notes ?? '',
        };

        this.dialogMode = 'edit';

        this.changeDetectorRef
            .markForCheck();
    }

    protected openDeleteDialog(
        asset: AssetResponse,
    ): void {
        this.clearMessages();
        this.resetMarketSearchState();

        this.selectedAsset = asset;
        this.dialogMode = 'delete';

        this.changeDetectorRef
            .markForCheck();
    }

    protected closeDialog(): void {
        if (
            this.isSubmitting
        ) {
            return;
        }

        this.dialogMode = null;
        this.selectedAsset = null;
        this.actionErrorMessage = '';

        this.resetMarketSearchState();

        this.changeDetectorRef
            .markForCheck();
    }

    protected submitCreate(): void {
        const request =
            this.buildRequest();

        if (
            !request
        ) {
            return;
        }

        if (
            !this.selectedPortfolioId
        ) {
            this.actionErrorMessage =
                this.text()
                    .assets
                    .selectPortfolioBeforeCreate;

            return;
        }

        this.startSubmission();

        this.assetService
            .createAsset(
                this.selectedPortfolioId,
                request,
            )
            .subscribe({
                next: () => {
                    this.finishSubmission();

                    this.dialogMode =
                        null;

                    this.selectedAsset =
                        null;

                    this.resetMarketSearchState();

                    this.successMessage =
                        this.text()
                            .assets
                            .createdSuccess;

                    this.changeDetectorRef
                        .markForCheck();

                    this.loadAssets(
                        false,
                    );
                },

                error: (
                    error: unknown,
                ) => {
                    this.finishSubmission();

                    this.actionErrorMessage =
                        this.resolveActionError(
                            error,
                            'create',
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    protected submitEdit(): void {
        const asset =
            this.selectedAsset;

        const request =
            this.buildRequest();

        if (
            !asset ||
            !request ||
            !this.selectedPortfolioId
        ) {
            return;
        }

        this.startSubmission();

        this.assetService
            .updateAsset(
                this.selectedPortfolioId,
                asset.id,
                request,
            )
            .subscribe({
                next: () => {
                    this.finishSubmission();

                    this.dialogMode =
                        null;

                    this.selectedAsset =
                        null;

                    this.resetMarketSearchState();

                    this.successMessage =
                        this.text()
                            .assets
                            .updatedSuccess;

                    this.changeDetectorRef
                        .markForCheck();

                    this.loadAssets(
                        false,
                    );
                },

                error: (
                    error: unknown,
                ) => {
                    this.finishSubmission();

                    this.actionErrorMessage =
                        this.resolveActionError(
                            error,
                            'edit',
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    protected submitDelete(): void {
        const asset =
            this.selectedAsset;

        if (
            !asset ||
            !this.selectedPortfolioId
        ) {
            return;
        }

        this.startSubmission();

        this.assetService
            .deleteAsset(
                this.selectedPortfolioId,
                asset.id,
            )
            .subscribe({
                next: () => {
                    this.finishSubmission();

                    this.dialogMode =
                        null;

                    this.selectedAsset =
                        null;

                    this.resetMarketSearchState();

                    this.successMessage =
                        this.text()
                            .assets
                            .deletedSuccess;

                    this.changeDetectorRef
                        .markForCheck();

                    this.loadAssets(
                        false,
                    );
                },

                error: (
                    error: unknown,
                ) => {
                    this.finishSubmission();

                    this.actionErrorMessage =
                        this.resolveActionError(
                            error,
                            'delete',
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    protected dismissSuccessMessage():
        void {
        this.successMessage = '';
    }

    protected get selectedPortfolio():
        PortfolioResponse | null {
        return (
            this.portfolios.find(
                (portfolio) =>
                    portfolio.id ===
                    this.selectedPortfolioId,
            ) ??
            null
        );
    }

    protected get uniqueCurrencies():
        number {
        return new Set(
            this.assets.map(
                (asset) =>
                    asset.currency,
            ),
        ).size;
    }

    protected get uniqueAssetTypes():
        number {
        return new Set(
            this.assets.map(
                (asset) =>
                    asset.assetType,
            ),
        ).size;
    }

    protected getAssetTypeLabel(
        assetType: AssetType,
    ): string {
        const translations =
            this.text().assets;

        switch (assetType) {
            case 'STOCK':
                return translations
                    .assetTypeStock;

            case 'ETF':
                return translations
                    .assetTypeEtf;

            case 'BOND':
                return translations
                    .assetTypeBond;

            case 'FUND':
                return translations
                    .assetTypeFund;

            case 'CRYPTO':
                return translations
                    .assetTypeCrypto;

            case 'COMMODITY':
                return translations
                    .assetTypeCommodity;

            case 'FOREX':
                return translations
                    .assetTypeForex;

            case 'CASH':
                return translations
                    .assetTypeCash;

            case 'OTHER':
                return translations
                    .assetTypeOther;
        }
    }

    private configureMarketSearch():
        void {
        this.marketSearchSubject
            .pipe(
                map(
                    (value) =>
                        value.trim(),
                ),

                debounceTime(
                    350,
                ),

                distinctUntilChanged(),

                tap((query) => {
                    if (
                        query.length < 2
                    ) {
                        this.isSearchingMarket =
                            false;

                        this.marketSearchResults =
                            [];

                        this.marketSearchMessage =
                            query.length === 1
                                ? this.text()
                                    .assets
                                    .enterAtLeastTwoCharacters
                                : '';

                        this.changeDetectorRef
                            .markForCheck();
                    }
                }),

                switchMap((query) => {
                    if (
                        query.length < 2
                    ) {
                        return of<
                            MarketSymbolSearchResponse[]
                        >([]);
                    }

                    this.isSearchingMarket =
                        true;

                    this.marketSearchResults =
                        [];

                    this.marketSearchMessage =
                        '';

                    this.changeDetectorRef
                        .markForCheck();

                    return this.marketDataService
                        .searchSymbols({
                            query,
                            limit: 10,
                        })
                        .pipe(
                            catchError(
                                (
                                    error:
                                        unknown,
                                ) => {
                                    this.marketSearchMessage =
                                        this.resolveMarketSearchError(
                                            error,
                                        );

                                    return of<
                                        MarketSymbolSearchResponse[]
                                    >([]);
                                },
                            ),

                            finalize(() => {
                                this.isSearchingMarket =
                                    false;

                                this.changeDetectorRef
                                    .markForCheck();
                            }),
                        );
                }),

                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe(
                (results) => {
                    this.marketSearchResults =
                        results;

                    if (
                        this.marketSearchValue
                            .trim()
                            .length >= 2 &&
                        results.length ===
                        0 &&
                        !this.marketSearchMessage
                    ) {
                        this.marketSearchMessage =
                            this.text()
                                .assets
                                .noMarketMatches;
                    }

                    this.changeDetectorRef
                        .markForCheck();
                },
            );
    }

    private loadLatestMarketPrice(
        result:
            MarketSymbolSearchResponse,
    ): void {
        this.isLoadingMarketPrice =
            true;

        this.selectedMarketPrice =
            null;

        this.changeDetectorRef
            .markForCheck();

        this.marketDataService
            .getLatestPrice({
                symbol:
                    result.symbol,
                exchange:
                    result.exchange,
            })
            .pipe(
                finalize(() => {
                    this.isLoadingMarketPrice =
                        false;

                    this.changeDetectorRef
                        .markForCheck();
                }),

                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: (
                    response:
                        MarketPriceResponse,
                ) => {
                    this.selectedMarketPrice =
                        response;

                    this.changeDetectorRef
                        .markForCheck();
                },

                error: (
                    error: unknown,
                ) => {
                    this.selectedMarketPrice =
                        null;

                    this.marketSearchMessage =
                        this.resolveMarketPriceError(
                            error,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    private resetMarketSearchState():
        void {
        this.marketSearchValue = '';
        this.marketSearchResults = [];
        this.selectedMarketResult = null;
        this.selectedMarketPrice = null;
        this.marketSearchMessage = '';
        this.isSearchingMarket = false;
        this.isLoadingMarketPrice = false;
    }

    private loadPortfolios(): void {
        this.isLoadingPortfolios =
            true;

        this.errorMessage = '';
        this.successMessage = '';

        this.changeDetectorRef
            .markForCheck();

        this.portfolioService
            .getPortfolios({
                page: 0,
                size: 100,
                sortBy: 'name',
                sortDirection: 'asc',
            })
            .subscribe({
                next: (
                    response:
                        PagedResponse<
                            PortfolioResponse
                        >,
                ) => {
                    this.portfolios =
                        response.content;

                    this.isLoadingPortfolios =
                        false;

                    if (
                        this.portfolios
                            .length > 0
                    ) {
                        this.selectedPortfolioId =
                            this.portfolios[0]
                                .id;

                        this.loadAssets();
                    } else {
                        this.selectedPortfolioId =
                            '';

                        this.assets =
                            [];

                        this.filteredAssets =
                            [];
                    }

                    this.changeDetectorRef
                        .markForCheck();
                },

                error: (
                    error: unknown,
                ) => {
                    this.isLoadingPortfolios =
                        false;

                    this.portfolios =
                        [];

                    this.assets =
                        [];

                    this.filteredAssets =
                        [];

                    this.selectedPortfolioId =
                        '';

                    this.errorMessage =
                        this.resolvePortfolioLoadError(
                            error,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    private loadAssets(
        clearMessages = true,
    ): void {
        if (
            !this.selectedPortfolioId
        ) {
            return;
        }

        this.isLoadingAssets =
            true;

        this.errorMessage = '';

        if (
            clearMessages
        ) {
            this.successMessage =
                '';
        }

        this.changeDetectorRef
            .markForCheck();

        this.assetService
            .getAssets(
                this.selectedPortfolioId,
            )
            .subscribe({
                next: (
                    assets:
                        AssetResponse[],
                ) => {
                    this.assets =
                        assets;

                    this.applySearchFilter();

                    this.isLoadingAssets =
                        false;

                    this.changeDetectorRef
                        .markForCheck();
                },

                error: (
                    error: unknown,
                ) => {
                    this.isLoadingAssets =
                        false;

                    this.assets =
                        [];

                    this.filteredAssets =
                        [];

                    this.errorMessage =
                        this.resolveAssetLoadError(
                            error,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    private applySearchFilter():
        void {
        const normalizedSearch =
            this.searchValue
                .trim()
                .toLowerCase();

        if (
            !normalizedSearch
        ) {
            this.filteredAssets = [
                ...this.assets,
            ];

            return;
        }

        this.filteredAssets =
            this.assets.filter(
                (asset) =>
                    asset.symbol
                        .toLowerCase()
                        .includes(
                            normalizedSearch,
                        ) ||
                    asset.displayName
                        .toLowerCase()
                        .includes(
                            normalizedSearch,
                        ) ||
                    asset.currency
                        .toLowerCase()
                        .includes(
                            normalizedSearch,
                        ) ||
                    asset.assetType
                        .toLowerCase()
                        .includes(
                            normalizedSearch,
                        ) ||
                    (
                        asset.exchange
                            ?.toLowerCase()
                            .includes(
                                normalizedSearch,
                            ) ??
                        false
                    ),
            );
    }

    private buildRequest():
        AssetCreateRequest |
        AssetUpdateRequest |
        null {
        const translations =
            this.text().assets;

        const symbol =
            this.formValue.symbol
                .trim()
                .toUpperCase();

        const displayName =
            this.formValue
                .displayName
                .trim();

        const currency =
            this.formValue.currency
                .trim()
                .toUpperCase();

        const isin =
            this.normalizeOptionalValue(
                this.formValue.isin,
                true,
            );

        const exchange =
            this.normalizeOptionalValue(
                this.formValue.exchange,
                false,
            );

        const notes =
            this.normalizeOptionalValue(
                this.formValue.notes,
                false,
            );

        if (
            !symbol
        ) {
            this.actionErrorMessage =
                translations
                    .symbolRequired;

            return null;
        }

        if (
            symbol.length > 30
        ) {
            this.actionErrorMessage =
                translations
                    .symbolTooLong;

            return null;
        }

        if (
            !displayName
        ) {
            this.actionErrorMessage =
                translations
                    .displayNameRequired;

            return null;
        }

        if (
            displayName.length >
            160
        ) {
            this.actionErrorMessage =
                translations
                    .displayNameTooLong;

            return null;
        }

        if (
            !/^[A-Z]{3}$/.test(
                currency,
            )
        ) {
            this.actionErrorMessage =
                translations
                    .currencyInvalid;

            return null;
        }

        if (
            isin !== null &&
            !/^[A-Z0-9]{12}$/.test(
                isin,
            )
        ) {
            this.actionErrorMessage =
                translations
                    .isinInvalid;

            return null;
        }

        if (
            exchange !== null &&
            exchange.length > 40
        ) {
            this.actionErrorMessage =
                translations
                    .exchangeTooLong;

            return null;
        }

        if (
            notes !== null &&
            notes.length > 2000
        ) {
            this.actionErrorMessage =
                translations
                    .notesTooLong;

            return null;
        }

        return {
            symbol,
            displayName,
            assetType:
                this.formValue
                    .assetType,
            currency,
            isin,
            exchange,
            notes,
        };
    }

    private normalizeOptionalValue(
        value: string,
        uppercase: boolean,
    ): string | null {
        const normalizedValue =
            value.trim();

        if (
            !normalizedValue
        ) {
            return null;
        }

        return uppercase
            ? normalizedValue
                .toUpperCase()
            : normalizedValue;
    }

    private createEmptyFormValue():
        AssetFormValue {
        return {
            symbol: '',
            displayName: '',
            assetType: 'STOCK',
            currency: 'USD',
            isin: '',
            exchange: '',
            notes: '',
        };
    }

    private startSubmission(): void {
        this.isSubmitting = true;
        this.actionErrorMessage = '';

        this.changeDetectorRef
            .markForCheck();
    }

    private finishSubmission(): void {
        this.isSubmitting = false;

        this.changeDetectorRef
            .markForCheck();
    }

    private resolveMarketSearchError(
        error: unknown,
    ): string {
        const translations =
            this.text().assets;

        if (
            !(
                error instanceof
                HttpErrorResponse
            )
        ) {
            return translations
                .marketSearchError;
        }

        if (
            error.status === 0
        ) {
            return translations
                .marketServiceUnavailable;
        }

        if (
            error.status === 401
        ) {
            return translations
                .sessionExpired;
        }

        if (
            error.status === 429
        ) {
            return translations
                .marketRateLimit;
        }

        if (
            error.status === 503
        ) {
            return translations
                .marketNotConfigured;
        }

        return (
            this.extractBackendMessage(
                error,
            ) ??
            translations
                .marketSearchError
        );
    }

    private resolveMarketPriceError(
        error: unknown,
    ): string {
        const translations =
            this.text().assets;

        if (
            !(
                error instanceof
                HttpErrorResponse
            )
        ) {
            return translations
                .marketPriceError;
        }

        if (
            error.status === 404
        ) {
            return translations
                .marketPriceNotFound;
        }

        if (
            error.status === 429
        ) {
            return translations
                .marketPriceRateLimit;
        }

        return (
            this.extractBackendMessage(
                error,
            ) ??
            translations
                .marketPriceError
        );
    }

    private resolvePortfolioLoadError(
        error: unknown,
    ): string {
        const translations =
            this.text().assets;

        if (
            !(
                error instanceof
                HttpErrorResponse
            )
        ) {
            return translations
                .portfolioLoadError;
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
                .forbiddenViewPortfolios;
        }

        return translations
            .portfolioLoadError;
    }

    private resolveAssetLoadError(
        error: unknown,
    ): string {
        const translations =
            this.text().assets;

        if (
            !(
                error instanceof
                HttpErrorResponse
            )
        ) {
            return translations
                .assetsLoadError;
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
                .forbiddenViewAssets;
        }

        if (
            error.status === 404
        ) {
            return translations
                .selectedPortfolioNotFound;
        }

        return translations
            .assetsLoadError;
    }

    private resolveActionError(
        error: unknown,
        action: AssetAction,
    ): string {
        const translations =
            this.text().assets;

        if (
            !(
                error instanceof
                HttpErrorResponse
            )
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
            error.status === 404
        ) {
            return translations
                .assetOrPortfolioNotFound;
        }

        if (
            error.status === 409
        ) {
            return translations
                .duplicateSymbol;
        }

        if (
            error.status === 400
        ) {
            return (
                this.extractBackendMessage(
                    error,
                ) ??
                translations
                    .invalidAsset
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
        error:
            HttpErrorResponse,
    ): string | null {
        const responseBody =
            error.error;

        if (
            responseBody &&
            typeof responseBody ===
            'object'
        ) {
            const candidate =
                'message' in
                    responseBody
                    ? responseBody
                        .message
                    : 'detail' in
                        responseBody
                        ? responseBody
                            .detail
                        : null;

            if (
                typeof candidate ===
                'string' &&
                candidate.trim()
            ) {
                return candidate
                    .trim();
            }
        }

        if (
            typeof responseBody ===
            'string' &&
            responseBody.trim()
        ) {
            return responseBody
                .trim();
        }

        return null;
    }

    private defaultActionError(
        action: AssetAction,
    ): string {
        const translations =
            this.text().assets;

        switch (
        action
        ) {
            case 'create':
                return translations
                    .createError;

            case 'edit':
                return translations
                    .editError;

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