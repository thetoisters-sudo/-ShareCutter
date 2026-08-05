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
    PagedResponse,
    PortfolioAllocationResponse,
    PortfolioCreateRequest,
    PortfolioRenameRequest,
    PortfolioResponse,
    PortfolioSummaryResponse,
} from '../../../../core/portfolio/models/portfolio.models';
import {
    PortfolioService,
} from '../../../../core/portfolio/services/portfolio.service';
import {
    PortfoliosPage,
} from './portfolios-page';

class PortfolioServiceStub {
    response: PagedResponse<PortfolioResponse> =
        createPagedResponse();

    summaries = new Map<
        string,
        PortfolioSummaryResponse
    >([
        [
            'portfolio-1',
            createPortfolioSummary(),
        ],
        [
            'portfolio-2',
            createPortfolioSummary({
                portfolioId: 'portfolio-2',
                portfolioName: 'Income Portfolio',
                initialValue: 8000,
                currentValue: 7600,
                totalRealizedProfit: 100,
                totalUnrealizedProfit: -500,
                totalProfit: -400,
                totalReturnPercent: -5,
                activeAssetCount: 2,
                transactionCount: 4,
            }),
        ],
    ]);

    allocations = new Map<
        string,
        PortfolioAllocationResponse
    >([
        [
            'portfolio-1',
            createPortfolioAllocation(),
        ],
        [
            'portfolio-2',
            createPortfolioAllocation({
                portfolioId: 'portfolio-2',
                portfolioName: 'Income Portfolio',
                totalMarketValue: 7600,
                totalCost: 8000,
                totalRealizedProfit: 100,
                totalUnrealizedProfit: -500,
                allocatedAssetCount: 0,
                assets: [],
            }),
        ],
    ]);

    getPortfolios = vi.fn(
        (): Observable<PagedResponse<PortfolioResponse>> =>
            of(this.response),
    );

    getPortfolioSummary = vi.fn(
        (
            portfolioId: string,
        ): Observable<PortfolioSummaryResponse> => {
            const summary =
                this.summaries.get(portfolioId);

            if (!summary) {
                return throwError(
                    () => new Error(
                        'Summary not found',
                    ),
                );
            }

            return of(summary);
        },
    );

    getPortfolioAllocation = vi.fn(
        (
            portfolioId: string,
        ): Observable<PortfolioAllocationResponse> => {
            const allocation =
                this.allocations.get(portfolioId);

            if (!allocation) {
                return throwError(
                    () => new Error(
                        'Allocation not found',
                    ),
                );
            }

            return of(allocation);
        },
    );

    createPortfolio = vi.fn(
        (
            _request: PortfolioCreateRequest,
        ): Observable<PortfolioResponse> =>
            of(createPortfolio()),
    );

    renamePortfolio = vi.fn(
        (
            _portfolioId: string,
            _request: PortfolioRenameRequest,
        ): Observable<PortfolioResponse> =>
            of(createPortfolio()),
    );

    deletePortfolio = vi.fn(
        (
            _portfolioId: string,
        ): Observable<void> =>
            of(undefined),
    );
}

describe('PortfoliosPage', () => {
    let fixture: ComponentFixture<PortfoliosPage>;
    let component: PortfoliosPage;
    let portfolioService: PortfolioServiceStub;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                PortfoliosPage,
            ],
            providers: [
                {
                    provide: PortfolioService,
                    useClass: PortfolioServiceStub,
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(
            PortfoliosPage,
        );

        component = fixture.componentInstance;

        portfolioService = TestBed.inject(
            PortfolioService,
        ) as unknown as PortfolioServiceStub;
    });

    it('should create', () => {
        fixture.detectChanges();

        expect(component).toBeTruthy();
    });

    it('should load summaries for every portfolio', () => {
        fixture.detectChanges();

        expect(
            portfolioService.getPortfolios,
        ).toHaveBeenCalledWith({
            page: 0,
            size: 6,
            sortBy: 'createdAt',
            sortDirection: 'desc',
        });

        expect(
            portfolioService.getPortfolioSummary,
        ).toHaveBeenCalledTimes(2);

        expect(
            portfolioService.getPortfolioSummary,
        ).toHaveBeenCalledWith(
            'portfolio-1',
        );

        expect(
            portfolioService.getPortfolioSummary,
        ).toHaveBeenCalledWith(
            'portfolio-2',
        );
    });

    it('should not load allocations during initial page loading', () => {
        fixture.detectChanges();

        expect(
            portfolioService.getPortfolioAllocation,
        ).not.toHaveBeenCalled();
    });

    it('should display calculated portfolio summaries', () => {
        fixture.detectChanges();

        const cards = fixture.debugElement.queryAll(
            By.css('.portfolio-card'),
        );

        expect(cards).toHaveLength(2);

        expect(
            fixture.nativeElement.textContent,
        ).toContain('Growth Portfolio');

        expect(
            fixture.nativeElement.textContent,
        ).toContain('Income Portfolio');

        expect(
            fixture.nativeElement.textContent,
        ).toContain('Live analytics');

        expect(
            fixture.nativeElement.textContent,
        ).toContain('Transactions');
    });

    it('should calculate displayed values from summaries', () => {
        fixture.detectChanges();

        const state = getComponentState(component);

        expect(state.totalCurrentValue).toBe(
            19100,
        );

        expect(state.displayedProfit).toBe(
            1100,
        );
    });

    it('should display the empty state without requesting summaries', () => {
        portfolioService.response = {
            ...createPagedResponse(),
            content: [],
            totalElements: 0,
            totalPages: 0,
            first: true,
            last: true,
        };

        fixture.detectChanges();

        expect(
            fixture.debugElement.query(
                By.css('.page-state'),
            ),
        ).not.toBeNull();

        expect(
            fixture.nativeElement.textContent,
        ).toContain('No portfolios yet');

        expect(
            portfolioService.getPortfolioSummary,
        ).not.toHaveBeenCalled();

        expect(
            portfolioService.getPortfolioAllocation,
        ).not.toHaveBeenCalled();
    });

    it('should display an error state when portfolio loading fails', () => {
        portfolioService.getPortfolios.mockReturnValueOnce(
            throwError(
                () => new Error('Request failed'),
            ),
        );

        fixture.detectChanges();

        expect(
            fixture.nativeElement.textContent,
        ).toContain('Portfolios unavailable');
    });

    it('should stop loading after a list error', () => {
        portfolioService.getPortfolios.mockReturnValueOnce(
            throwError(
                () => new HttpErrorResponse({
                    status: 500,
                    statusText: 'Server Error',
                }),
            ),
        );

        fixture.detectChanges();

        const state = getComponentState(component);

        expect(state.isLoading).toBe(false);

        expect(state.errorMessage).toBe(
            'Portfolios could not be loaded. Please try again.',
        );
    });

    it('should use cached portfolio data when one summary fails', () => {
        portfolioService.getPortfolioSummary.mockImplementation(
            (
                portfolioId: string,
            ): Observable<PortfolioSummaryResponse> => {
                if (portfolioId === 'portfolio-2') {
                    return throwError(
                        () => new Error(
                            'Summary failed',
                        ),
                    );
                }

                return of(
                    createPortfolioSummary(),
                );
            },
        );

        fixture.detectChanges();

        const state = getComponentState(component);

        expect(state.portfolios).toHaveLength(2);

        expect(
            state.analyticsUnavailableCount,
        ).toBe(1);

        expect(
            state.portfolios[0].analyticsAvailable,
        ).toBe(true);

        expect(
            state.portfolios[1].analyticsAvailable,
        ).toBe(false);

        expect(
            state.portfolios[1].currentValue,
        ).toBe(7600);

        expect(
            fixture.nativeElement.textContent,
        ).toContain(
            'Cached portfolio values are shown temporarily.',
        );
    });

    it('should request the next page', () => {
        portfolioService.response = {
            ...createPagedResponse(),
            first: true,
            last: false,
            totalPages: 2,
        };

        fixture.detectChanges();

        const nextButton =
            fixture.debugElement.queryAll(
                By.css('.pagination button'),
            )[1];

        nextButton.triggerEventHandler(
            'click',
        );

        expect(
            portfolioService.getPortfolios,
        ).toHaveBeenLastCalledWith({
            page: 1,
            size: 6,
            sortBy: 'createdAt',
            sortDirection: 'desc',
        });
    });

    it('should load allocation when a portfolio is expanded', () => {
        fixture.detectChanges();

        const state = getComponentState(component);
        const portfolio = state.portfolios[0];

        togglePortfolioDetails(
            component,
            portfolio,
        );

        fixture.detectChanges();

        expect(
            portfolioService.getPortfolioAllocation,
        ).toHaveBeenCalledTimes(1);

        expect(
            portfolioService.getPortfolioAllocation,
        ).toHaveBeenCalledWith(
            portfolio.id,
        );

        expect(
            state.expandedPortfolioId,
        ).toBe(portfolio.id);

        expect(
            state.getPortfolioAllocation(
                portfolio.id,
            ),
        ).toEqual(
            createPortfolioAllocation(),
        );
    });

    it('should display allocation assets after expanding a portfolio', () => {
        fixture.detectChanges();

        const state = getComponentState(component);

        togglePortfolioDetails(
            component,
            state.portfolios[0],
        );

        fixture.detectChanges();

        expect(
            fixture.nativeElement.textContent,
        ).toContain('Holdings and weights');

        expect(
            fixture.nativeElement.textContent,
        ).toContain('AAPL');

        expect(
            fixture.nativeElement.textContent,
        ).toContain('Apple Inc.');

        expect(
            fixture.nativeElement.textContent,
        ).toContain('MSFT');

        expect(
            fixture.nativeElement.textContent,
        ).toContain('Microsoft Corporation');

        const rows = fixture.debugElement.queryAll(
            By.css('.allocation-table tbody tr'),
        );

        expect(rows).toHaveLength(2);
    });

    it('should display the empty allocation state', () => {
        fixture.detectChanges();

        const state = getComponentState(component);

        togglePortfolioDetails(
            component,
            state.portfolios[1],
        );

        fixture.detectChanges();

        expect(
            fixture.nativeElement.textContent,
        ).toContain('No allocated assets');

        expect(
            fixture.debugElement.query(
                By.css('.allocation-table'),
            ),
        ).toBeNull();
    });

    it('should close an expanded portfolio', () => {
        fixture.detectChanges();

        const state = getComponentState(component);
        const portfolio = state.portfolios[0];

        togglePortfolioDetails(
            component,
            portfolio,
        );

        fixture.detectChanges();

        expect(
            state.expandedPortfolioId,
        ).toBe(portfolio.id);

        togglePortfolioDetails(
            component,
            portfolio,
        );

        fixture.detectChanges();

        expect(
            state.expandedPortfolioId,
        ).toBeNull();

        expect(
            fixture.debugElement.query(
                By.css('.portfolio-allocation'),
            ),
        ).toBeNull();
    });

    it('should reuse cached allocation after closing and reopening', () => {
        fixture.detectChanges();

        const state = getComponentState(component);
        const portfolio = state.portfolios[0];

        togglePortfolioDetails(
            component,
            portfolio,
        );

        togglePortfolioDetails(
            component,
            portfolio,
        );

        togglePortfolioDetails(
            component,
            portfolio,
        );

        fixture.detectChanges();

        expect(
            portfolioService.getPortfolioAllocation,
        ).toHaveBeenCalledTimes(1);

        expect(
            state.getPortfolioAllocation(
                portfolio.id,
            ),
        ).not.toBeNull();
    });

    it('should display an allocation error', () => {
        portfolioService
            .getPortfolioAllocation
            .mockReturnValueOnce(
                throwError(
                    () => new Error(
                        'Allocation failed',
                    ),
                ),
            );

        fixture.detectChanges();

        const state = getComponentState(component);
        const portfolio = state.portfolios[0];

        togglePortfolioDetails(
            component,
            portfolio,
        );

        fixture.detectChanges();

        expect(
            state.getAllocationError(
                portfolio.id,
            ),
        ).toBe(
            'Portfolio allocation could not be loaded. Please try again.',
        );

        expect(
            fixture.nativeElement.textContent,
        ).toContain('Allocation unavailable');

        expect(
            fixture.nativeElement.textContent,
        ).toContain(
            'Portfolio allocation could not be loaded. Please try again.',
        );
    });

    it('should retry allocation loading after an error', () => {
        portfolioService
            .getPortfolioAllocation
            .mockReturnValueOnce(
                throwError(
                    () => new Error(
                        'Allocation failed',
                    ),
                ),
            )
            .mockReturnValueOnce(
                of(
                    createPortfolioAllocation(),
                ),
            );

        fixture.detectChanges();

        const state = getComponentState(component);
        const portfolio = state.portfolios[0];

        togglePortfolioDetails(
            component,
            portfolio,
        );

        fixture.detectChanges();

        expect(
            state.getAllocationError(
                portfolio.id,
            ),
        ).not.toBe('');

        retryPortfolioAllocation(
            component,
            portfolio.id,
        );

        fixture.detectChanges();

        expect(
            portfolioService.getPortfolioAllocation,
        ).toHaveBeenCalledTimes(2);

        expect(
            state.getAllocationError(
                portfolio.id,
            ),
        ).toBe('');

        expect(
            state.getPortfolioAllocation(
                portfolio.id,
            ),
        ).toEqual(
            createPortfolioAllocation(),
        );
    });

    it('should switch between expanded portfolios', () => {
        fixture.detectChanges();

        const state = getComponentState(component);

        togglePortfolioDetails(
            component,
            state.portfolios[0],
        );

        togglePortfolioDetails(
            component,
            state.portfolios[1],
        );

        fixture.detectChanges();

        expect(
            state.expandedPortfolioId,
        ).toBe('portfolio-2');

        expect(
            portfolioService.getPortfolioAllocation,
        ).toHaveBeenCalledTimes(2);

        expect(
            fixture.debugElement.queryAll(
                By.css('.portfolio-allocation'),
            ),
        ).toHaveLength(1);
    });

    it('should clear allocation cache when portfolios reload', () => {
        fixture.detectChanges();

        const state = getComponentState(component);
        const portfolio = state.portfolios[0];

        togglePortfolioDetails(
            component,
            portfolio,
        );

        expect(
            state.getPortfolioAllocation(
                portfolio.id,
            ),
        ).not.toBeNull();

        state.retry();

        fixture.detectChanges();

        expect(
            state.expandedPortfolioId,
        ).toBeNull();

        expect(
            state.getPortfolioAllocation(
                portfolio.id,
            ),
        ).toBeNull();
    });

    it('should create a portfolio', () => {
        fixture.detectChanges();

        openCreateDialog(component);

        setCreateValues(
            component,
            'New Portfolio',
            5000,
        );

        submitCreate(component);

        expect(
            portfolioService.createPortfolio,
        ).toHaveBeenCalledWith({
            name: 'New Portfolio',
            creationMethod: 'BY_AMOUNT',
            initialValue: 5000,
        });
    });

    it('should stop submitting after a 409 create error', () => {
        portfolioService.createPortfolio.mockReturnValueOnce(
            throwError(
                () => new HttpErrorResponse({
                    status: 409,
                    statusText: 'Conflict',
                    error: {
                        message:
                            'Portfolio name already exists.',
                    },
                }),
            ),
        );

        fixture.detectChanges();

        openCreateDialog(component);

        setCreateValues(
            component,
            'Growth Portfolio',
            5000,
        );

        submitCreate(component);

        const state = getComponentState(component);

        expect(state.isSubmitting).toBe(false);
        expect(state.dialogMode).toBe('create');

        expect(state.actionErrorMessage).toBe(
            'A portfolio with this name already exists.',
        );
    });

    it('should display the duplicate-name message in the create dialog', () => {
        const state = getComponentState(component);

        state.dialogMode = 'create';
        state.actionErrorMessage =
            'A portfolio with this name already exists.';
        state.isSubmitting = false;

        fixture.detectChanges();

        expect(
            fixture.nativeElement.textContent,
        ).toContain(
            'A portfolio with this name already exists.',
        );

        const submitButton =
            fixture.debugElement.query(
                By.css(
                    '.portfolio-form button[type="submit"]',
                ),
            );

        expect(submitButton).not.toBeNull();

        expect(
            submitButton.nativeElement.textContent.trim(),
        ).toBe('Create portfolio');

        expect(
            submitButton.nativeElement.disabled,
        ).toBe(false);
    });

    it('should stop submitting after an unknown create error', () => {
        portfolioService.createPortfolio.mockReturnValueOnce(
            throwError(
                () => new Error('Request failed'),
            ),
        );

        fixture.detectChanges();

        openCreateDialog(component);

        setCreateValues(
            component,
            'New Portfolio',
            5000,
        );

        submitCreate(component);

        const state = getComponentState(component);

        expect(state.isSubmitting).toBe(false);

        expect(state.actionErrorMessage).toBe(
            'The portfolio could not be created.',
        );
    });

    it('should rename a portfolio', () => {
        fixture.detectChanges();

        const state = getComponentState(component);
        const portfolio = state.portfolios[0];

        openRenameDialog(
            component,
            portfolio,
        );

        setRenameValue(
            component,
            'Renamed Portfolio',
        );

        submitRename(component);

        expect(
            portfolioService.renamePortfolio,
        ).toHaveBeenCalledWith(
            portfolio.id,
            {
                name: 'Renamed Portfolio',
            },
        );
    });

    it('should stop submitting after a rename conflict', () => {
        portfolioService.renamePortfolio.mockReturnValueOnce(
            throwError(
                () => new HttpErrorResponse({
                    status: 409,
                    statusText: 'Conflict',
                }),
            ),
        );

        fixture.detectChanges();

        const state = getComponentState(component);
        const portfolio = state.portfolios[0];

        openRenameDialog(
            component,
            portfolio,
        );

        setRenameValue(
            component,
            'Income Portfolio',
        );

        submitRename(component);

        expect(state.isSubmitting).toBe(false);

        expect(state.actionErrorMessage).toBe(
            'A portfolio with this name already exists.',
        );
    });

    it('should delete a portfolio', () => {
        fixture.detectChanges();

        const state = getComponentState(component);
        const portfolio = state.portfolios[0];

        openDeleteDialog(
            component,
            portfolio,
        );

        submitDelete(component);

        expect(
            portfolioService.deletePortfolio,
        ).toHaveBeenCalledWith(
            portfolio.id,
        );
    });

    it('should not expose a manual value update action', () => {
        fixture.detectChanges();

        expect(
            fixture.nativeElement.textContent,
        ).not.toContain('Update value');

        expect(
            fixture.debugElement.queryAll(
                By.css(
                    '.portfolio-card__actions button',
                ),
            ),
        ).toHaveLength(6);
    });
});

interface PortfolioListItemTest {
    id: string;
    userId: string;
    name: string;
    creationMethod:
    | 'BY_AMOUNT'
    | 'BY_HOLDINGS';
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

interface PortfoliosPageTestState {
    portfolios: PortfolioListItemTest[];
    isLoading: boolean;
    isSubmitting: boolean;
    errorMessage: string;
    actionErrorMessage: string;
    analyticsUnavailableCount: number;
    totalCurrentValue: number;
    displayedProfit: number;
    dialogMode:
    | 'create'
    | 'rename'
    | 'delete'
    | null;
    expandedPortfolioId: string | null;
    createName: string;
    createInitialValue: number | null;
    renameValue: string;
    retry(): void;
    openCreateDialog(): void;
    submitCreate(): void;
    openRenameDialog(
        portfolio: PortfolioListItemTest,
    ): void;
    submitRename(): void;
    openDeleteDialog(
        portfolio: PortfolioListItemTest,
    ): void;
    submitDelete(): void;
    togglePortfolioDetails(
        portfolio: PortfolioListItemTest,
    ): void;
    retryPortfolioAllocation(
        portfolioId: string,
    ): void;
    getPortfolioAllocation(
        portfolioId: string,
    ): PortfolioAllocationResponse | null;
    getAllocationError(
        portfolioId: string,
    ): string;
}

function getComponentState(
    component: PortfoliosPage,
): PortfoliosPageTestState {
    return component as unknown as PortfoliosPageTestState;
}

function openCreateDialog(
    component: PortfoliosPage,
): void {
    getComponentState(component).openCreateDialog();
}

function setCreateValues(
    component: PortfoliosPage,
    name: string,
    initialValue: number,
): void {
    const state = getComponentState(component);

    state.createName = name;
    state.createInitialValue = initialValue;
}

function submitCreate(
    component: PortfoliosPage,
): void {
    getComponentState(component).submitCreate();
}

function openRenameDialog(
    component: PortfoliosPage,
    portfolio: PortfolioListItemTest,
): void {
    getComponentState(component)
        .openRenameDialog(portfolio);
}

function setRenameValue(
    component: PortfoliosPage,
    name: string,
): void {
    getComponentState(component).renameValue = name;
}

function submitRename(
    component: PortfoliosPage,
): void {
    getComponentState(component).submitRename();
}

function openDeleteDialog(
    component: PortfoliosPage,
    portfolio: PortfolioListItemTest,
): void {
    getComponentState(component)
        .openDeleteDialog(portfolio);
}

function submitDelete(
    component: PortfoliosPage,
): void {
    getComponentState(component).submitDelete();
}

function togglePortfolioDetails(
    component: PortfoliosPage,
    portfolio: PortfolioListItemTest,
): void {
    getComponentState(component)
        .togglePortfolioDetails(portfolio);
}

function retryPortfolioAllocation(
    component: PortfoliosPage,
    portfolioId: string,
): void {
    getComponentState(component)
        .retryPortfolioAllocation(portfolioId);
}

function createPagedResponse():
    PagedResponse<PortfolioResponse> {
    return {
        content: [
            createPortfolio(),
            {
                ...createPortfolio(),
                id: 'portfolio-2',
                name: 'Income Portfolio',
                initialValue: 8000,
                currentValue: 7600,
                totalRealizedProfit: 100,
                totalUnrealizedProfit: -500,
                totalReturnPercent: -5,
            },
        ],
        page: 0,
        size: 6,
        totalElements: 2,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
    };
}

function createPortfolio(): PortfolioResponse {
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

function createPortfolioSummary(
    overrides:
        Partial<PortfolioSummaryResponse> = {},
): PortfolioSummaryResponse {
    return {
        portfolioId: 'portfolio-1',
        portfolioName: 'Growth Portfolio',
        initialValue: 10000,
        currentValue: 11500,
        totalRealizedProfit: 500,
        totalUnrealizedProfit: 1000,
        totalProfit: 1500,
        totalReturnPercent: 15,
        activeAssetCount: 3,
        transactionCount: 8,
        totalBuyAmount: 10000,
        totalSellAmount: 0,
        totalDividendAmount: 0,
        totalFeeAmount: 0,
        totalDepositAmount: 0,
        totalWithdrawalAmount: 0,
        netCashFlow: 10000,
        calculatedAt: '2026-07-30T20:00:00Z',
        ...overrides,
    };
}

function createPortfolioAllocation(
    overrides:
        Partial<PortfolioAllocationResponse> = {},
): PortfolioAllocationResponse {
    return {
        portfolioId: 'portfolio-1',
        portfolioName: 'Growth Portfolio',
        totalMarketValue: 11500,
        totalCost: 10000,
        totalRealizedProfit: 500,
        totalUnrealizedProfit: 1000,
        allocatedAssetCount: 2,
        assets: [
            {
                assetId: 'asset-1',
                symbol: 'AAPL',
                displayName: 'Apple Inc.',
                assetType: 'STOCK',
                currency: 'USD',
                quantity: 5.5,
                averageCost: 180,
                currentPrice: 195,
                totalCost: 990,
                marketValue: 1072.5,
                realizedProfit: 50,
                unrealizedProfit: 82.5,
                allocationPercent: 9.32608696,
            },
            {
                assetId: 'asset-2',
                symbol: 'MSFT',
                displayName:
                    'Microsoft Corporation',
                assetType: 'STOCK',
                currency: 'USD',
                quantity: 20,
                averageCost: 450.5,
                currentPrice: 521.375,
                totalCost: 9010,
                marketValue: 10427.5,
                realizedProfit: 450,
                unrealizedProfit: 917.5,
                allocationPercent: 90.67391304,
            },
        ],
        calculatedAt: '2026-07-30T20:00:00Z',
        ...overrides,
    };
}