import {
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
} from '@angular/common';
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
    RouterLink,
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
    TranslationService,
} from '../../../../core/i18n/services/translation.service';
import {
    PortfolioAllocationResponse,
    PortfolioCreationMethod,
    PortfolioResponse,
    PortfolioSummaryResponse,
} from '../../../../core/portfolio/models/portfolio.models';
import {
    PortfolioService,
} from '../../../../core/portfolio/services/portfolio.service';

interface DashboardPortfolio {
    id: string;
    name: string;
    creationMethod: PortfolioCreationMethod;
    initialValue: number;
    currentValue: number;
    cashBalance: number;
    holdingsMarketValue: number;
    cashPercent: number;
    investedPercent: number;
    totalRealizedProfit: number;
    totalUnrealizedProfit: number;
    totalProfit: number;
    totalReturnPercent: number;
    calculatedAt: string;
    analyticsAvailable: boolean;
    allocationAvailable: boolean;
}

@Component({
    selector: 'app-dashboard-page',
    imports: [
        CurrencyPipe,
        DatePipe,
        DecimalPipe,
        RouterLink,
    ],
    templateUrl:
        './dashboard-page.html',
    styleUrl:
        './dashboard-page.scss',
    changeDetection:
        ChangeDetectionStrategy.OnPush,
})
export class DashboardPage
    implements OnInit {

    private readonly portfolioService =
        inject(PortfolioService);

    private readonly translationService =
        inject(TranslationService);

    private readonly changeDetectorRef =
        inject(ChangeDetectorRef);

    private readonly destroyRef =
        inject(DestroyRef);

    protected readonly text =
        this.translationService.text;

    protected portfolios:
        DashboardPortfolio[] = [];

    protected isLoading = true;

    protected errorMessage = '';

    ngOnInit(): void {
        this.subscribeToPortfolioChanges();
        this.loadPortfolios();
    }

    protected retry(): void {
        this.loadPortfolios();
    }

    protected trackPortfolioById(
        _index: number,
        portfolio: DashboardPortfolio,
    ): string {
        return portfolio.id;
    }

    protected get portfolioCount(): number {
        return this.portfolios.length;
    }

    protected get analyticsUnavailableCount(): number {
        return this.portfolios.filter(
            (portfolio) =>
                !portfolio.analyticsAvailable,
        ).length;
    }

    protected get allocationUnavailableCount(): number {
        return this.portfolios.filter(
            (portfolio) =>
                !portfolio.allocationAvailable,
        ).length;
    }

    protected get totalInitialValue(): number {
        return this.portfolios.reduce(
            (
                total,
                portfolio,
            ) =>
                total +
                portfolio.initialValue,
            0,
        );
    }

    protected get totalCurrentValue(): number {
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

    protected get totalCashBalance(): number {
        return this.portfolios.reduce(
            (
                total,
                portfolio,
            ) =>
                total +
                portfolio.cashBalance,
            0,
        );
    }

    protected get totalHoldingsMarketValue(): number {
        return this.portfolios.reduce(
            (
                total,
                portfolio,
            ) =>
                total +
                portfolio.holdingsMarketValue,
            0,
        );
    }

    protected get totalProfit(): number {
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

    protected get totalReturnPercent(): number {
        if (
            this.totalInitialValue === 0
        ) {
            return 0;
        }

        return (
            this.totalProfit /
            this.totalInitialValue
        ) * 100;
    }

    protected get totalCashPercent(): number {
        return this.calculatePercent(
            this.totalCashBalance,
            this.totalCurrentValue,
        );
    }

    protected get totalInvestedPercent(): number {
        return this.calculatePercent(
            this.totalHoldingsMarketValue,
            this.totalCurrentValue,
        );
    }

    protected get bestPerformingPortfolio():
        DashboardPortfolio | null {
        if (
            this.portfolios.length === 0
        ) {
            return null;
        }

        return this.portfolios.reduce(
            (
                bestPortfolio,
                currentPortfolio,
            ) =>
                (
                    currentPortfolio
                        .totalReturnPercent >
                    bestPortfolio
                        .totalReturnPercent
                )
                    ? currentPortfolio
                    : bestPortfolio,
        );
    }

    protected isPositiveOrZero(
        value: number,
    ): boolean {
        return value >= 0;
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
            .subscribe(() => {
                this.loadPortfolios();
            });
    }

    private loadPortfolios(): void {
        this.isLoading = true;
        this.errorMessage = '';

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
                switchMap((response) => {
                    if (
                        response.content.length ===
                        0
                    ) {
                        return of<
                            DashboardPortfolio[]
                        >([]);
                    }

                    return forkJoin(
                        response.content.map(
                            (portfolio) =>
                                this.loadPortfolioData(
                                    portfolio,
                                ),
                        ),
                    );
                }),
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: (portfolios) => {
                    this.portfolios =
                        portfolios;

                    this.isLoading =
                        false;

                    this.changeDetectorRef
                        .markForCheck();
                },

                error: () => {
                    this.portfolios = [];
                    this.isLoading = false;

                    this.errorMessage =
                        this.text()
                            .dashboard
                            .loadError;

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    private loadPortfolioData(
        portfolio: PortfolioResponse,
    ): Observable<DashboardPortfolio> {
        return forkJoin({
            summary:
                this.portfolioService
                    .getPortfolioSummary(
                        portfolio.id,
                    )
                    .pipe(
                        map((summary) => ({
                            value:
                                summary,

                            available:
                                true,
                        })),

                        catchError(() =>
                            of({
                                value:
                                    null,

                                available:
                                    false,
                            }),
                        ),
                    ),

            allocation:
                this.portfolioService
                    .getPortfolioAllocation(
                        portfolio.id,
                    )
                    .pipe(
                        map((allocation) => ({
                            value:
                                allocation,

                            available:
                                true,
                        })),

                        catchError(() =>
                            of({
                                value:
                                    null,

                                available:
                                    false,
                            }),
                        ),
                    ),
        }).pipe(
            map(
                ({
                    summary,
                    allocation,
                }) =>
                    this.createDashboardPortfolio(
                        portfolio,
                        summary.value,
                        summary.available,
                        allocation.value,
                        allocation.available,
                    ),
            ),
        );
    }

    private createDashboardPortfolio(
        portfolio: PortfolioResponse,
        summary:
            PortfolioSummaryResponse | null,
        analyticsAvailable: boolean,
        allocation:
            PortfolioAllocationResponse | null,
        allocationAvailable: boolean,
    ): DashboardPortfolio {
        const currentValue =
            summary?.currentValue ??
            portfolio.currentValue;

        const cashBalance =
            allocation?.cashBalance ??
            0;

        const holdingsMarketValue =
            allocation?.totalMarketValue ??
            0;

        return {
            id:
                portfolio.id,

            name:
                summary?.portfolioName ??
                portfolio.name,

            creationMethod:
                portfolio.creationMethod,

            initialValue:
                summary?.initialValue ??
                portfolio.initialValue,

            currentValue,

            cashBalance,

            holdingsMarketValue,

            cashPercent:
                this.calculatePercent(
                    cashBalance,
                    currentValue,
                ),

            investedPercent:
                this.calculatePercent(
                    holdingsMarketValue,
                    currentValue,
                ),

            totalRealizedProfit:
                (
                    summary
                        ?.totalRealizedProfit ??
                    portfolio
                        .totalRealizedProfit
                ),

            totalUnrealizedProfit:
                (
                    summary
                        ?.totalUnrealizedProfit ??
                    portfolio
                        .totalUnrealizedProfit
                ),

            totalProfit:
                summary?.totalProfit ??
                (
                    portfolio.currentValue -
                    portfolio.initialValue
                ),

            totalReturnPercent:
                (
                    summary
                        ?.totalReturnPercent ??
                    portfolio
                        .totalReturnPercent
                ),

            calculatedAt:
                allocation?.calculatedAt ??
                summary?.calculatedAt ??
                portfolio.updatedAt,

            analyticsAvailable,

            allocationAvailable,
        };
    }

    private calculatePercent(
        value: number,
        total: number,
    ): number {
        if (
            total <= 0
        ) {
            return 0;
        }

        return (
            value /
            total
        ) * 100;
    }
}