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
    vi,
} from 'vitest';

import {
    AssetCreateRequest,
    AssetResponse,
    AssetUpdateRequest,
} from '../../../../core/asset/models/asset.models';
import {
    AssetService,
} from '../../../../core/asset/services/asset.service';
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
    let portfolioService: PortfolioServiceStub;

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

        portfolioService = TestBed.inject(
            PortfolioService,
        ) as unknown as PortfolioServiceStub;
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
    isLoadingAssets: boolean;
    isSubmitting: boolean;
    actionErrorMessage: string;
    formValue: {
        symbol: string;
        displayName: string;
        assetType:
        | 'STOCK'
        | 'ETF'
        | 'BOND'
        | 'FUND'
        | 'CRYPTO'
        | 'COMMODITY'
        | 'FOREX'
        | 'CASH'
        | 'OTHER';
        currency: string;
        isin: string;
        exchange: string;
        notes: string;
    };
    onSearchChange(): void;
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
        createdAt: '2026-07-20T10:00:00Z',
        updatedAt: '2026-07-29T10:00:00Z',
    };
}

function createAsset(): AssetResponse {
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
        createdAt: '2026-07-30T10:00:00Z',
        updatedAt: '2026-07-30T10:00:00Z',
    };
}