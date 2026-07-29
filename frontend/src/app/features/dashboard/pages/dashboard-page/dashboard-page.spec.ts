import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import {
    PagedResponse,
    PortfolioResponse,
} from '../../../../core/portfolio/models/portfolio.models';
import {
    PortfolioService,
} from '../../../../core/portfolio/services/portfolio.service';
import { DashboardPage } from './dashboard-page';

class PortfolioServiceStub {
    response$: Observable<
        PagedResponse<PortfolioResponse>
    > = of(createPagedResponse());

    getPortfolios(): Observable<
        PagedResponse<PortfolioResponse>
    > {
        return this.response$;
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

    it('should load and display portfolio data', () => {
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

    it('should calculate combined portfolio values', () => {
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
        ).toBe(16500);

        expect(
            getComponentValue<number>(
                component,
                'totalProfit',
            ),
        ).toBe(1500);

        expect(
            getComponentValue<number>(
                component,
                'totalReturnPercent',
            ),
        ).toBe(10);
    });

    it('should identify the best performing portfolio', () => {
        createComponent();

        const bestPortfolio =
            getComponentValue<PortfolioResponse | null>(
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
    });

    it('should display an error state when loading fails', () => {
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