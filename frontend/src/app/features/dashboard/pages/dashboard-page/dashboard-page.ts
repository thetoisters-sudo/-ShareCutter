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
    ChartConfiguration,
    ChartData,
} from 'chart.js';
import {
    BaseChartDirective,
} from 'ng2-charts';

import {
    TranslationService,
} from '../../../../core/i18n/services/translation.service';
import {
    PortfolioAllocationResponse,
    PortfolioCreationMethod,
    PortfolioHistoryPointResponse,
    PortfolioResponse,
    PortfolioSummaryResponse,
} from '../../../../core/portfolio/models/portfolio.models';
import {
    PortfolioMarketRefreshResponse,
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
    allocationAssets:
    PortfolioAllocationResponse['assets'];
    history:
    PortfolioHistoryPointResponse[];
    historyAvailable: boolean;
}

@Component({
    selector: 'app-dashboard-page',
    imports: [
        CurrencyPipe,
        DatePipe,
        DecimalPipe,
        RouterLink,
        BaseChartDirective,
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

    protected isRefreshingMarketData = false;

    protected lastMarketRefreshAt: string | null =
        null;

    protected marketRefreshFailedSymbols:
        string[] = [];

    protected marketRefreshMessage = '';

    protected marketRefreshErrorMessage = '';

    ngOnInit(): void {
        this.subscribeToPortfolioChanges();
        this.loadPortfolios();
    }

    protected retry(): void {
        this.loadPortfolios();
    }

    protected refreshMarketData(): void {
        if (this.isRefreshingMarketData) {
            return;
        }

        this.isRefreshingMarketData = true;
        this.marketRefreshFailedSymbols = [];
        this.marketRefreshMessage = '';
        this.marketRefreshErrorMessage = '';

        this.changeDetectorRef
            .markForCheck();

        this.portfolioService
            .refreshMarketData()
            .pipe(
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: (
                    response:
                        PortfolioMarketRefreshResponse,
                ) => {
                    this.lastMarketRefreshAt =
                        response.refreshedAt;

                    this.marketRefreshFailedSymbols =
                        response.failedSymbols;

                    this.marketRefreshMessage =
                        response.failedSymbols.length > 0
                            ? this.text().dashboard
                                .marketRefreshPartial
                            : this.text().dashboard
                                .marketRefreshSuccess;

                    this.isRefreshingMarketData =
                        false;

                    this.portfolioService
                        .notifyPortfolioChanged({
                            portfolioId: null,
                            reason:
                                'market-price-updated',
                        });

                    this.changeDetectorRef
                        .markForCheck();
                },

                error: () => {
                    this.isRefreshingMarketData =
                        false;

                    this.marketRefreshErrorMessage =
                        this.text().dashboard
                            .marketRefreshFailed;

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
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

    protected getPortfolioAllocationChartData(
        portfolio: DashboardPortfolio,
    ): ChartData<
        'doughnut',
        number[],
        string
    > {
        const labels: string[] = [];
        const values: number[] = [];

        if (portfolio.cashBalance > 0) {
            labels.push(
                this.text().dashboard.cash,
            );

            values.push(
                portfolio.cashBalance,
            );
        }

        const sortedAssets =
            [...portfolio.allocationAssets]
                .filter(
                    (asset) =>
                        asset.marketValue > 0,
                )
                .sort(
                    (
                        leftAsset,
                        rightAsset,
                    ) =>
                        rightAsset.marketValue -
                        leftAsset.marketValue,
                );

        for (const asset of sortedAssets) {
            labels.push(asset.symbol);
            values.push(asset.marketValue);
        }

        return {
            labels,
            datasets: [
                {
                    data: values,
                },
            ],
        };
    }

    protected readonly allocationChartOptions:
        ChartConfiguration<'doughnut'>['options'] = {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '62%',
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        usePointStyle: true,
                        boxWidth: 10,
                        boxHeight: 10,
                        padding: 18,
                    },
                },
                tooltip: {
                    callbacks: {
                        label: (context) => {
                            const label =
                                context.label ?? '';

                            const value =
                                Number(
                                    context.raw ?? 0,
                                );

                            const total =
                                context.dataset.data
                                    .reduce(
                                        (
                                            sum,
                                            item,
                                        ) =>
                                            sum +
                                            Number(item),
                                        0,
                                    );

                            const percent =
                                total > 0
                                    ? (
                                        value /
                                        total
                                    ) * 100
                                    : 0;

                            return (
                                `${label}: ` +
                                `$${value.toLocaleString(
                                    undefined,
                                    {
                                        minimumFractionDigits:
                                            2,
                                        maximumFractionDigits:
                                            2,
                                    },
                                )} ` +
                                `(${percent.toFixed(2)}%)`
                            );
                        },
                    },
                },
            },
        };

    protected hasPortfolioAllocationData(
        portfolio: DashboardPortfolio,
    ): boolean {
        return (
            portfolio.cashBalance > 0 ||
            portfolio.allocationAssets.some(
                (asset) =>
                    asset.marketValue > 0,
            )
        );
    }

    protected get cashVsInvestedChartData():
        ChartData<'bar', number[], string> {
        return {
            labels:
                this.portfolios.map(
                    (portfolio) =>
                        portfolio.name,
                ),

            datasets: [
                {
                    label:
                        this.text().dashboard.cash,
                    data:
                        this.portfolios.map(
                            (portfolio) =>
                                portfolio.cashBalance,
                        ),
                },
                {
                    label:
                        this.text().dashboard.holdings,
                    data:
                        this.portfolios.map(
                            (portfolio) =>
                                portfolio.holdingsMarketValue,
                        ),
                },
            ],
        };
    }

    protected readonly cashVsInvestedChartOptions:
        ChartConfiguration<'bar'>['options'] = {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false,
            },
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        usePointStyle: true,
                        boxWidth: 10,
                        boxHeight: 10,
                        padding: 18,
                    },
                },
                tooltip: {
                    callbacks: {
                        label: (context) => {
                            const label =
                                context.dataset.label ??
                                '';

                            const value =
                                Number(
                                    context.raw ?? 0,
                                );

                            return (
                                `${label}: ` +
                                `$${value.toLocaleString(
                                    undefined,
                                    {
                                        minimumFractionDigits:
                                            2,
                                        maximumFractionDigits:
                                            2,
                                    },
                                )}`
                            );
                        },
                    },
                },
            },
            scales: {
                x: {
                    stacked: false,
                    ticks: {
                        autoSkip: false,
                    },
                },
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: (value) =>
                            `$${Number(value).toLocaleString(
                                undefined,
                                {
                                    maximumFractionDigits:
                                        0,
                                },
                            )}`,
                    },
                },
            },
        };

    protected getPortfolioGrowthChartData(
        portfolio: DashboardPortfolio,
    ): ChartData<
        'line',
        number[],
        string
    > {
        return {
            labels:
                portfolio.history.map(
                    (point) =>
                        this.formatHistoryLabel(
                            point.capturedAt,
                        ),
                ),

            datasets: [
                {
                    label:
                        this.text().dashboard
                            .portfolioValue,
                    data:
                        portfolio.history.map(
                            (point) =>
                                point.currentValue,
                        ),
                    tension: 0.25,
                    fill: false,
                    pointRadius: 3,
                    pointHoverRadius: 5,
                },
            ],
        };
    }

    protected readonly portfolioGrowthChartOptions:
        ChartConfiguration<'line'>['options'] = {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false,
            },
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        usePointStyle: true,
                        boxWidth: 10,
                        boxHeight: 10,
                        padding: 18,
                    },
                },
                tooltip: {
                    callbacks: {
                        label: (context) => {
                            const value =
                                Number(
                                    context.raw ?? 0,
                                );

                            return (
                                `${this.text().dashboard.portfolioValue}: ` +
                                `$${value.toLocaleString(
                                    undefined,
                                    {
                                        minimumFractionDigits:
                                            2,
                                        maximumFractionDigits:
                                            2,
                                    },
                                )}`
                            );
                        },
                    },
                },
            },
            scales: {
                x: {
                    ticks: {
                        maxRotation: 45,
                        minRotation: 0,
                    },
                },
                y: {
                    beginAtZero: false,
                    ticks: {
                        callback: (value) =>
                            `$${Number(value).toLocaleString(
                                undefined,
                                {
                                    maximumFractionDigits:
                                        0,
                                },
                            )}`,
                    },
                },
            },
        };

    protected hasPortfolioGrowthData(
        portfolio: DashboardPortfolio,
    ): boolean {
        return portfolio.history.length > 1;
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

            history:
                this.portfolioService
                    .getPortfolioHistory(
                        portfolio.id,
                    )
                    .pipe(
                        map((history) => ({
                            value:
                                history,

                            available:
                                true,
                        })),

                        catchError(() =>
                            of({
                                value:
                                    [] as
                                    PortfolioHistoryPointResponse[],

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
                    history,
                }) =>
                    this.createDashboardPortfolio(
                        portfolio,
                        summary.value,
                        summary.available,
                        allocation.value,
                        allocation.available,
                        history.value,
                        history.available,
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
        history:
            PortfolioHistoryPointResponse[],
        historyAvailable: boolean,
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

            allocationAssets:
                allocation?.assets ?? [],

            history,

            historyAvailable,
        };
    }

    private formatHistoryLabel(
        capturedAt: string,
    ): string {
        const date =
            new Date(capturedAt);

        if (
            Number.isNaN(
                date.getTime(),
            )
        ) {
            return capturedAt;
        }

        return date.toLocaleString(
            undefined,
            {
                month: 'short',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
            },
        );
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