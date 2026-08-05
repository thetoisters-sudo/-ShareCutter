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
    takeUntilDestroyed,
} from '@angular/core/rxjs-interop';

import {
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
    totalRealizedProfit: number;
    totalUnrealizedProfit: number;
    totalProfit: number;
    totalReturnPercent: number;
    calculatedAt: string;
    analyticsAvailable: boolean;
}

@Component({
    selector: 'app-dashboard-page',
    imports: [
        CurrencyPipe,
        DatePipe,
        DecimalPipe,
        RouterLink,
    ],
    templateUrl: './dashboard-page.html',
    styleUrl: './dashboard-page.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPage implements OnInit {
    private readonly portfolioService =
        inject(PortfolioService);

    private readonly changeDetectorRef =
        inject(ChangeDetectorRef);

    private readonly destroyRef =
        inject(DestroyRef);

    protected portfolios: DashboardPortfolio[] = [];
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

    protected get totalInitialValue(): number {
        return this.portfolios.reduce(
            (
                total,
                portfolio,
            ) => total + portfolio.initialValue,
            0,
        );
    }

    protected get totalCurrentValue(): number {
        return this.portfolios.reduce(
            (
                total,
                portfolio,
            ) => total + portfolio.currentValue,
            0,
        );
    }

    protected get totalProfit(): number {
        return this.portfolios.reduce(
            (
                total,
                portfolio,
            ) => total + portfolio.totalProfit,
            0,
        );
    }

    protected get totalReturnPercent(): number {
        if (this.totalInitialValue === 0) {
            return 0;
        }

        return (
            this.totalProfit /
            this.totalInitialValue
        ) * 100;
    }

    protected get bestPerformingPortfolio():
        DashboardPortfolio | null {
        if (this.portfolios.length === 0) {
            return null;
        }

        return this.portfolios.reduce(
            (
                bestPortfolio,
                currentPortfolio,
            ) =>
                currentPortfolio.totalReturnPercent >
                    bestPortfolio.totalReturnPercent
                    ? currentPortfolio
                    : bestPortfolio,
        );
    }

    protected isPositiveOrZero(
        value: number,
    ): boolean {
        return value >= 0;
    }

    private subscribeToPortfolioChanges(): void {
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
        this.changeDetectorRef.markForCheck();

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
                        response.content.length === 0
                    ) {
                        return of<
                            DashboardPortfolio[]
                        >([]);
                    }

                    return forkJoin(
                        response.content.map(
                            (portfolio) =>
                                this.loadPortfolioSummary(
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
                    this.portfolios = portfolios;
                    this.isLoading = false;

                    this.changeDetectorRef
                        .markForCheck();
                },
                error: () => {
                    this.portfolios = [];
                    this.isLoading = false;
                    this.errorMessage =
                        'Portfolio data could not be loaded. Please try again.';

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    private loadPortfolioSummary(
        portfolio: PortfolioResponse,
    ): Observable<DashboardPortfolio> {
        return this.portfolioService
            .getPortfolioSummary(
                portfolio.id,
            )
            .pipe(
                map((summary) =>
                    this.createDashboardPortfolio(
                        portfolio,
                        summary,
                    ),
                ),
                catchError(() =>
                    of(
                        this.createFallbackPortfolio(
                            portfolio,
                        ),
                    ),
                ),
            );
    }

    private createDashboardPortfolio(
        portfolio: PortfolioResponse,
        summary: PortfolioSummaryResponse,
    ): DashboardPortfolio {
        return {
            id: portfolio.id,
            name: summary.portfolioName,
            creationMethod:
                portfolio.creationMethod,
            initialValue:
                summary.initialValue,
            currentValue:
                summary.currentValue,
            totalRealizedProfit:
                summary.totalRealizedProfit,
            totalUnrealizedProfit:
                summary.totalUnrealizedProfit,
            totalProfit:
                summary.totalProfit,
            totalReturnPercent:
                summary.totalReturnPercent,
            calculatedAt:
                summary.calculatedAt,
            analyticsAvailable: true,
        };
    }

    private createFallbackPortfolio(
        portfolio: PortfolioResponse,
    ): DashboardPortfolio {
        return {
            id: portfolio.id,
            name: portfolio.name,
            creationMethod:
                portfolio.creationMethod,
            initialValue:
                portfolio.initialValue,
            currentValue:
                portfolio.currentValue,
            totalRealizedProfit:
                portfolio.totalRealizedProfit,
            totalUnrealizedProfit:
                portfolio.totalUnrealizedProfit,
            totalProfit:
                portfolio.currentValue -
                portfolio.initialValue,
            totalReturnPercent:
                portfolio.totalReturnPercent,
            calculatedAt:
                portfolio.updatedAt,
            analyticsAvailable: false,
        };
    }
}