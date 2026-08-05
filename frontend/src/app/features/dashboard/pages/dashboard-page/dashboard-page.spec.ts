import {
    ComponentFixture,
    TestBed,
} from '@angular/core/testing';
import {
    provideRouter,
} from '@angular/router';
import {
    Observable,
    of,
    throwError,
} from 'rxjs';

import {
    PagedResponse,
    PortfolioResponse,
    PortfolioSummaryResponse,
} from '../../../../core/portfolio/models/portfolio.models';
import {
    PortfolioService,
} from '../../../../core/portfolio/services/portfolio.service';
import {
    DashboardPage,
} from './dashboard-page';

class PortfolioServiceStub {
    response$: Observable<
        PagedResponse<PortfolioResponse>
    > = of(createPagedResponse());

    summaryResponses = new Map<
        string,
        Observable<PortfolioSummaryResponse>
    >([
        [
            'portfolio-1',
            of(createGrowthSummary()),
        ],
        [
            'portfolio-2',
            of(createIncomeSummary()),
        ],
    ]);

    summaryRequestIds: string[] = [];

    getPortfolios(): Observable<
        PagedResponse<PortfolioResponse>
    > {
        return this.response$;
    }

    getPortfolioSummary(
        portfolioId: string,
    ): Observable<PortfolioSummaryResponse> {
        this.summaryRequestIds.push(portfolioId);

        return (
            this.summaryResponses.get(portfolioId) ??
            throwError(
                () => new Error(
                    'Summary not configured',
                ),
            )
        );
    }
}

describe('DashboardPage', () => {
    let fixture: ComponentFixture<DashboardPage>;
    let component: DashboardPage;
    let portfolioService: PortfolioServiceStub;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                DashboardPage,
            ],
            providers: [
                provideRouter([]),
                {
                    provide: PortfolioService,
                    useClass: PortfolioServiceStub,
                },
            ],
        }).compileComponents();

        portfolioService =
            TestBed.inject(
                PortfolioService,
            ) as unknown as PortfolioServiceStub;
    });

    it('should create', () => {
        createComponent();

        expect(component).toBeTruthy();
    });

    it('should load summaries for every portfolio', () => {
        createComponent();

        expect(
            portfolioService.summaryRequestIds,
        ).toEqual([
            'portfolio-1',
            'portfolio-2',
        ]);
    });

    it('should load and display calculated portfolio data', () => {
        createComponent();

        const textContent =
            fixture.nativeElement.textContent;

        expect(textContent).toContain(
            'Growth portfolio',
        );

        expect(textContent).toContain(
            'Income portfolio',
        );

        expect(textContent).toContain(
            '2',
        );
    });

    it('should calculate combined values from portfolio summaries', () => {
        createComponent();

        expect(
            getComponentValue<number>(
                component,
                'totalInitialValue',
            ),
        ).toBe(15000);

        expect(
            getComponentValue<number>(
                component,
                'totalCurrentValue',
            ),
        ).toBe(16800);

        expect(
            getComponentValue<number>(
                component,
                'totalProfit',
            ),
        ).toBe(1800);

        expect(
            getComponentValue<number>(
                component,
                'totalReturnPercent',
            ),
        ).toBe(12);
    });

    it('should identify the best performing summary', () => {
        createComponent();

        const bestPortfolio =
            getComponentValue<{
                name: string;
            } | null>(
                component,
                'bestPerformingPortfolio',
            );

        expect(bestPortfolio?.name).toBe(
            'Growth portfolio',
        );
    });

    it('should display an empty state when there are no portfolios', () => {
        portfolioService.response$ =
            of(
                createPagedResponse([]),
            );

        createComponent();

        expect(
            fixture.nativeElement.textContent,
        ).toContain(
            'No portfolios yet',
        );

        expect(
            portfolioService.summaryRequestIds,
        ).toEqual([]);
    });

    it('should use cached portfolio data when one summary fails', () => {
        portfolioService.summaryResponses.set(
            'portfolio-2',
            throwError(
                () => new Error(
                    'Summary request failed',
                ),
            ),
        );

        createComponent();

        expect(
            getComponentValue<number>(
                component,
                'totalCurrentValue',
            ),
        ).toBe(17000);

        expect(
            getComponentValue<number>(
                component,
                'analyticsUnavailableCount',
            ),
        ).toBe(1);

        expect(
            fixture.nativeElement.textContent,
        ).toContain(
            'Cached portfolio values',
        );
    });

    it('should display an error state when portfolio loading fails', () => {
        portfolioService.response$ =
            throwError(
                () => new Error(
                    'Request failed',
                ),
            );

        createComponent();

        expect(
            fixture.nativeElement.textContent,
        ).toContain(
            'Dashboard unavailable',
        );

        expect(
            fixture.nativeElement.textContent,
        ).toContain(
            'Portfolio data could not be loaded',
        );
    });

    function createComponent(): void {
        fixture =
            TestBed.createComponent(
                DashboardPage,
            );

        component = fixture.componentInstance;

        fixture.detectChanges();
    }
});

function createPagedResponse(
    content: PortfolioResponse[] = createPortfolios(),
): PagedResponse<PortfolioResponse> {
    return {
        content,
        page: 0,
        size: 100,
        totalElements: content.length,
        totalPages: content.length > 0 ? 1 : 0,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
    };
}

function createPortfolios(): PortfolioResponse[] {
    return [
        {
            id: 'portfolio-1',
            userId: 'user-1',
            name: 'Growth portfolio',
            creationMethod: 'BY_AMOUNT',
            initialValue: 10000,
            currentValue: 11500,
            totalRealizedProfit: 500,
            totalUnrealizedProfit: 1000,
            totalReturnPercent: 15,
            createdAt: '2026-07-01T10:00:00Z',
            updatedAt: '2026-07-29T10:00:00Z',
        },
        {
            id: 'portfolio-2',
            userId: 'user-1',
            name: 'Income portfolio',
            creationMethod: 'BY_HOLDINGS',
            initialValue: 5000,
            currentValue: 5000,
            totalRealizedProfit: 100,
            totalUnrealizedProfit: -100,
            totalReturnPercent: 0,
            createdAt: '2026-07-02T10:00:00Z',
            updatedAt: '2026-07-28T10:00:00Z',
        },
    ];
}

function createGrowthSummary():
    PortfolioSummaryResponse {
    return {
        portfolioId: 'portfolio-1',
        portfolioName: 'Growth portfolio',
        initialValue: 10000,
        currentValue: 12000,
        totalRealizedProfit: 600,
        totalUnrealizedProfit: 1400,
        totalProfit: 2000,
        totalReturnPercent: 20,
        activeAssetCount: 4,
        transactionCount: 12,
        totalBuyAmount: 12500,
        totalSellAmount: 2500,
        totalDividendAmount: 100,
        totalFeeAmount: 25,
        totalDepositAmount: 10000,
        totalWithdrawalAmount: 0,
        netCashFlow: 10000,
        calculatedAt: '2026-07-30T20:00:00Z',
    };
}

function createIncomeSummary():
    PortfolioSummaryResponse {
    return {
        portfolioId: 'portfolio-2',
        portfolioName: 'Income portfolio',
        initialValue: 5000,
        currentValue: 4800,
        totalRealizedProfit: 100,
        totalUnrealizedProfit: -300,
        totalProfit: -200,
        totalReturnPercent: -4,
        activeAssetCount: 2,
        transactionCount: 5,
        totalBuyAmount: 5200,
        totalSellAmount: 0,
        totalDividendAmount: 0,
        totalFeeAmount: 10,
        totalDepositAmount: 5000,
        totalWithdrawalAmount: 0,
        netCashFlow: 5000,
        calculatedAt: '2026-07-30T20:01:00Z',
    };
}

function getComponentValue<T>(
    component: DashboardPage,
    propertyName: string,
): T {
    return (
        component as unknown as Record<
            string,
            T
        >
    )[propertyName];
}