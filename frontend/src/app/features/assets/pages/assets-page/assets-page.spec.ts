import {
    HttpErrorResponse,
} from '@angular/common/http';
import {
    ComponentFixture,
    TestBed,
} from '@angular/core/testing';
import {
    By,
} from '@angular/platform-browser';
import {
    Observable,
    of,
    throwError,
} from 'rxjs';
import {
    afterEach,
    vi,
} from 'vitest';

import {
    AssetCreateRequest,
    AssetResponse,
    AssetType,
    AssetUpdateRequest,
} from '../../../../core/asset/models/asset.models';
import {
    AssetService,
} from '../../../../core/asset/services/asset.service';
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
import {
    AssetsPage,
} from './assets-page';

class AssetServiceStub {
    response: AssetResponse[] = [
        createAsset(),
        {
            ...createAsset(),
            id: 'asset-2',
            symbol: 'BTC',
            displayName: 'Bitcoin',
            assetType: 'CRYPTO',
            currency: 'USD',
            isin: null,
            exchange: 'Coinbase',
        },
    ];

    getAssets = vi.fn(
        (
            _portfolioId: string,
        ): Observable<AssetResponse[]> =>
            of(this.response),
    );

    createAsset = vi.fn(
        (
            _portfolioId: string,
            _request: AssetCreateRequest,
        ): Observable<AssetResponse> =>
            of(createAsset()),
    );

    updateAsset = vi.fn(
        (
            _portfolioId: string,
            _assetId: string,
            _request: AssetUpdateRequest,
        ): Observable<AssetResponse> =>
            of(createAsset()),
    );

    deleteAsset = vi.fn(
        (
            _portfolioId: string,
            _assetId: string,
        ): Observable<void> =>
            of(undefined),
    );
}

class MarketDataServiceStub {
    searchResponse:
        MarketSymbolSearchResponse[] = [
            createMarketSearchResult(),
        ];

    priceResponse:
        MarketPriceResponse = {
            symbol: 'TTWO',
            exchange: 'NASDAQ',
            price: 205.55,
            retrievedAt:
                '2026-08-05T13:00:00Z',
        };

    searchSymbols = vi.fn(
        (
            _request: {
                query: string;
                limit?: number;
            },
        ): Observable<
            MarketSymbolSearchResponse[]
        > =>
            of(this.searchResponse),
    );

    getLatestPrice = vi.fn(
        (
            _request: {
                symbol: string;
                exchange?: string | null;
            },
        ): Observable<MarketPriceResponse> =>
            of(this.priceResponse),
    );
}

class PortfolioServiceStub {
    response:
        PagedResponse<PortfolioResponse> = {
            content: [
                createPortfolio(),
                {
                    ...createPortfolio(),
                    id: 'portfolio-2',
                    name: 'Income Portfolio',
                },
            ],
            page: 0,
            size: 100,
            totalElements: 2,
            totalPages: 1,
            first: true,
            last: true,
            hasNext: false,
            hasPrevious: false,
        };

    getPortfolios = vi.fn(
        (): Observable<
            PagedResponse<PortfolioResponse>
        > =>
            of(this.response),
    );
}

describe('AssetsPage', () => {
    let fixture: ComponentFixture<AssetsPage>;
    let component: AssetsPage;
    let assetService: AssetServiceStub;
    let marketDataService:
        MarketDataServiceStub;
    let portfolioService:
        PortfolioServiceStub;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                AssetsPage,
            ],
            providers: [
                {
                    provide: AssetService,
                    useClass: AssetServiceStub,
                },
                {
                    provide: MarketDataService,
                    useClass: MarketDataServiceStub,
                },
                {
                    provide: PortfolioService,
                    useClass: PortfolioServiceStub,
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(
            AssetsPage,
        );

        component = fixture.componentInstance;

        assetService = TestBed.inject(
            AssetService,
        ) as unknown as AssetServiceStub;

        marketDataService = TestBed.inject(
            MarketDataService,
        ) as unknown as MarketDataServiceStub;

        portfolioService = TestBed.inject(
            PortfolioService,
        ) as unknown as PortfolioServiceStub;
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('should create', () => {
        fixture.detectChanges();

        expect(component).toBeTruthy();
    });

    it('should load portfolios and assets', () => {
        fixture.detectChanges();

        expect(
            portfolioService.getPortfolios,
        ).toHaveBeenCalledWith({
            page: 0,
            size: 100,
            sortBy: 'name',
            sortDirection: 'asc',
        });

        expect(
            assetService.getAssets,
        ).toHaveBeenCalledWith(
            'portfolio-1',
        );

        const cards =
            fixture.debugElement.queryAll(
                By.css('.asset-card'),
            );

        expect(cards).toHaveLength(2);

        expect(
            fixture.nativeElement.textContent,
        ).toContain('AAPL');

        expect(
            fixture.nativeElement.textContent,
        ).toContain('Bitcoin');
    });

    it('should display the no-portfolio state', () => {
        portfolioService.response = {
            ...portfolioService.response,
            content: [],
            totalElements: 0,
            totalPages: 0,
        };

        fixture.detectChanges();

        expect(
            fixture.nativeElement.textContent,
        ).toContain(
            'No portfolios available',
        );

        expect(
            assetService.getAssets,
        ).not.toHaveBeenCalled();
    });

    it('should display the empty asset state', () => {
        assetService.response = [];

        fixture.detectChanges();

        expect(
            fixture.nativeElement.textContent,
        ).toContain('No assets yet');
    });

    it('should filter assets by search value', () => {
        fixture.detectChanges();

        const searchInput =
            fixture.debugElement.query(
                By.css(
                    'input[name="searchValue"]',
                ),
            );

        expect(searchInput).not.toBeNull();

        searchInput.nativeElement.value =
            'bitcoin';

        searchInput.nativeElement.dispatchEvent(
            new Event('input'),
        );

        fixture.detectChanges();

        const cards =
            fixture.debugElement.queryAll(
                By.css('.asset-card'),
            );

        expect(cards).toHaveLength(1);

        expect(
            fixture.nativeElement.textContent,
        ).toContain('BTC');

        expect(
            fixture.nativeElement.textContent,
        ).not.toContain('AAPL');
    });

    it('should display market search inside the create dialog', () => {
        fixture.detectChanges();

        const state = getComponentState(component);

        state.openCreateDialog();
        fixture.detectChanges();

        expect(
            fixture.debugElement.query(
                By.css('.market-search'),
            ),
        ).not.toBeNull();

        expect(
            fixture.nativeElement.textContent,
        ).toContain(
            'Find a listed instrument',
        );
    });

    it('should search market instruments after the debounce period', () => {
        vi.useFakeTimers();

        fixture.detectChanges();

        const state = getComponentState(component);

        state.openCreateDialog();
        state.onMarketSearchChange(
            '  Take Two  ',
        );

        expect(
            marketDataService.searchSymbols,
        ).not.toHaveBeenCalled();

        vi.advanceTimersByTime(349);

        expect(
            marketDataService.searchSymbols,
        ).not.toHaveBeenCalled();

        vi.advanceTimersByTime(1);

        expect(
            marketDataService.searchSymbols,
        ).toHaveBeenCalledWith({
            query: 'Take Two',
            limit: 10,
        });

        expect(
            state.marketSearchResults,
        ).toEqual([
            createMarketSearchResult(),
        ]);
    });

    it('should not search a one-character market query', () => {
        vi.useFakeTimers();

        fixture.detectChanges();

        const state = getComponentState(component);

        state.openCreateDialog();
        state.onMarketSearchChange('T');

        vi.advanceTimersByTime(350);

        expect(
            marketDataService.searchSymbols,
        ).not.toHaveBeenCalled();

        expect(
            state.marketSearchMessage,
        ).toBe(
            'Enter at least 2 characters.',
        );
    });

    it('should fill the asset form and load price after selecting a market result', () => {
        fixture.detectChanges();

        const state = getComponentState(component);
        const result =
            createMarketSearchResult();

        state.openCreateDialog();
        state.selectMarketResult(result);

        expect(state.formValue).toEqual({
            symbol: 'TTWO',
            displayName:
                'Take-Two Interactive Software Inc.',
            assetType: 'STOCK',
            currency: 'USD',
            isin: '',
            exchange: 'NASDAQ',
            notes: '',
        });

        expect(
            marketDataService.getLatestPrice,
        ).toHaveBeenCalledWith({
            symbol: 'TTWO',
            exchange: 'NASDAQ',
        });

        expect(
            state.selectedMarketResult,
        ).toEqual(result);

        expect(
            state.selectedMarketPrice,
        ).toEqual(
            marketDataService.priceResponse,
        );
    });

    it('should display a selected market instrument and its price', () => {
        fixture.detectChanges();

        const state = getComponentState(component);

        state.openCreateDialog();

        state.selectMarketResult(
            createMarketSearchResult(),
        );

        fixture.detectChanges();

        const selectedMarket =
            fixture.debugElement.query(
                By.css('.selected-market'),
            );

        expect(selectedMarket).not.toBeNull();

        expect(
            selectedMarket.nativeElement
                .textContent,
        ).toContain('TTWO');

        expect(
            selectedMarket.nativeElement
                .textContent,
        ).toContain('205.55');

        expect(
            selectedMarket.nativeElement
                .textContent,
        ).toContain('USD');
    });

    it('should clear the selected market result', () => {
        fixture.detectChanges();

        const state = getComponentState(component);

        state.openCreateDialog();

        state.selectMarketResult(
            createMarketSearchResult(),
        );

        state.clearSelectedMarketResult();

        expect(
            state.selectedMarketResult,
        ).toBeNull();

        expect(
            state.selectedMarketPrice,
        ).toBeNull();

        expect(
            state.marketSearchValue,
        ).toBe('');

        expect(
            state.marketSearchResults,
        ).toEqual([]);
    });

    it('should allow manual entry when market search fails', () => {
        vi.useFakeTimers();

        marketDataService
            .searchSymbols
            .mockReturnValueOnce(
                throwError(
                    () =>
                        new HttpErrorResponse({
                            status: 503,
                            statusText:
                                'Service Unavailable',
                        }),
                ),
            );

        fixture.detectChanges();

        const state = getComponentState(component);

        state.openCreateDialog();
        state.onMarketSearchChange('TTWO');

        vi.advanceTimersByTime(350);

        expect(
            state.marketSearchResults,
        ).toEqual([]);

        expect(
            state.marketSearchMessage,
        ).toBe(
            'Market data is not configured. ' +
            'You can continue with manual entry.',
        );
    });

    it('should create an asset', () => {
        fixture.detectChanges();

        const state = getComponentState(component);

        state.openCreateDialog();

        state.formValue = {
            symbol: ' msft ',
            displayName:
                ' Microsoft Corporation ',
            assetType: 'STOCK',
            currency: ' usd ',
            isin: ' us5949181045 ',
            exchange: 'NASDAQ',
            notes: 'Technology holding',
        };

        state.submitCreate();

        expect(
            assetService.createAsset,
        ).toHaveBeenCalledWith(
            'portfolio-1',
            {
                symbol: 'MSFT',
                displayName:
                    'Microsoft Corporation',
                assetType: 'STOCK',
                currency: 'USD',
                isin: 'US5949181045',
                exchange: 'NASDAQ',
                notes: 'Technology holding',
            },
        );
    });

    it('should create a market-selected asset', () => {
        fixture.detectChanges();

        const state = getComponentState(component);

        state.openCreateDialog();

        state.selectMarketResult(
            createMarketSearchResult(),
        );

        state.submitCreate();

        expect(
            assetService.createAsset,
        ).toHaveBeenCalledWith(
            'portfolio-1',
            {
                symbol: 'TTWO',
                displayName:
                    'Take-Two Interactive Software Inc.',
                assetType: 'STOCK',
                currency: 'USD',
                isin: null,
                exchange: 'NASDAQ',
                notes: null,
            },
        );
    });

    it('should reject an invalid currency', () => {
        fixture.detectChanges();

        const state = getComponentState(component);

        state.openCreateDialog();

        state.formValue = {
            symbol: 'AAPL',
            displayName: 'Apple Inc.',
            assetType: 'STOCK',
            currency: 'US',
            isin: '',
            exchange: '',
            notes: '',
        };

        state.submitCreate();

        expect(
            assetService.createAsset,
        ).not.toHaveBeenCalled();

        expect(
            state.actionErrorMessage,
        ).toBe(
            'Currency must contain exactly 3 letters.',
        );
    });

    it('should edit an asset', () => {
        fixture.detectChanges();

        const asset = createAsset();
        const state = getComponentState(component);

        state.openEditDialog(asset);

        state.formValue.displayName =
            'Updated Apple Inc.';

        state.submitEdit();

        expect(
            assetService.updateAsset,
        ).toHaveBeenCalledWith(
            'portfolio-1',
            asset.id,
            {
                symbol: 'AAPL',
                displayName:
                    'Updated Apple Inc.',
                assetType: 'STOCK',
                currency: 'USD',
                isin: 'US0378331005',
                exchange: 'NASDAQ',
                notes:
                    'Core technology holding',
            },
        );
    });

    it('should not display market search inside the edit dialog', () => {
        fixture.detectChanges();

        const state = getComponentState(component);

        state.openEditDialog(
            createAsset(),
        );

        fixture.detectChanges();

        expect(
            fixture.debugElement.query(
                By.css('.market-search'),
            ),
        ).toBeNull();
    });

    it('should delete an asset', () => {
        fixture.detectChanges();

        const asset = createAsset();
        const state = getComponentState(component);

        state.openDeleteDialog(asset);
        state.submitDelete();

        expect(
            assetService.deleteAsset,
        ).toHaveBeenCalledWith(
            'portfolio-1',
            asset.id,
        );
    });

    it('should stop submitting after a conflict', () => {
        assetService.createAsset.mockReturnValueOnce(
            throwError(
                () => new HttpErrorResponse({
                    status: 409,
                    statusText: 'Conflict',
                }),
            ),
        );

        fixture.detectChanges();

        const state = getComponentState(component);

        state.openCreateDialog();

        state.formValue = {
            symbol: 'AAPL',
            displayName: 'Apple Inc.',
            assetType: 'STOCK',
            currency: 'USD',
            isin: '',
            exchange: '',
            notes: '',
        };

        state.submitCreate();

        expect(state.isSubmitting).toBe(false);

        expect(
            state.actionErrorMessage,
        ).toBe(
            'An asset with this symbol already exists in the portfolio.',
        );
    });

    it('should display an asset load error', () => {
        assetService.getAssets.mockReturnValueOnce(
            throwError(
                () => new HttpErrorResponse({
                    status: 500,
                    statusText: 'Server Error',
                }),
            ),
        );

        fixture.detectChanges();

        expect(
            fixture.nativeElement.textContent,
        ).toContain('Assets unavailable');

        expect(
            getComponentState(component)
                .isLoadingAssets,
        ).toBe(false);
    });
});

interface AssetsPageTestState {
    selectedPortfolioId: string;
    searchValue: string;
    marketSearchValue: string;
    isLoadingAssets: boolean;
    isSubmitting: boolean;
    isSearchingMarket: boolean;
    isLoadingMarketPrice: boolean;
    actionErrorMessage: string;
    marketSearchMessage: string;

    marketSearchResults:
    MarketSymbolSearchResponse[];

    selectedMarketResult:
    MarketSymbolSearchResponse | null;

    selectedMarketPrice:
    MarketPriceResponse | null;

    formValue: {
        symbol: string;
        displayName: string;
        assetType: AssetType;
        currency: string;
        isin: string;
        exchange: string;
        notes: string;
    };

    onSearchChange(): void;

    onMarketSearchChange(
        value: string,
    ): void;

    selectMarketResult(
        result: MarketSymbolSearchResponse,
    ): void;

    clearSelectedMarketResult(): void;

    openCreateDialog(): void;

    submitCreate(): void;

    openEditDialog(
        asset: AssetResponse,
    ): void;

    submitEdit(): void;

    openDeleteDialog(
        asset: AssetResponse,
    ): void;

    submitDelete(): void;
}

function getComponentState(
    component: AssetsPage,
): AssetsPageTestState {
    return component as unknown as AssetsPageTestState;
}

function createPortfolio():
    PortfolioResponse {
    return {
        id: 'portfolio-1',
        userId: 'user-1',
        name: 'Growth Portfolio',
        creationMethod: 'BY_AMOUNT',
        initialValue: 10000,
        currentValue: 11500,
        totalRealizedProfit: 500,
        totalUnrealizedProfit: 1000,
        totalReturnPercent: 15,
        createdAt:
            '2026-07-20T10:00:00Z',
        updatedAt:
            '2026-07-29T10:00:00Z',
    };
}

function createAsset():
    AssetResponse {
    return {
        id: 'asset-1',
        portfolioId: 'portfolio-1',
        symbol: 'AAPL',
        displayName: 'Apple Inc.',
        assetType: 'STOCK',
        currency: 'USD',
        isin: 'US0378331005',
        exchange: 'NASDAQ',
        notes: 'Core technology holding',
        createdAt:
            '2026-07-30T10:00:00Z',
        updatedAt:
            '2026-07-30T10:00:00Z',
    };
}

function createMarketSearchResult():
    MarketSymbolSearchResponse {
    return {
        symbol: 'TTWO',
        displayName:
            'Take-Two Interactive Software Inc.',
        exchange: 'NASDAQ',
        micCode: 'XNAS',
        instrumentType:
            'Common Stock',
        country: 'United States',
        currency: 'USD',
        assetType: 'STOCK',
    };
}