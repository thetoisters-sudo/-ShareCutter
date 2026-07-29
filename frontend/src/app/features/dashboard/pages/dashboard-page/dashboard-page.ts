import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import {
    PortfolioResponse,
} from '../../../../core/portfolio/models/portfolio.models';
import {
    PortfolioService,
} from '../../../../core/portfolio/services/portfolio.service';

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
})
export class DashboardPage implements OnInit {
    private readonly portfolioService =
        inject(PortfolioService);

    protected portfolios: PortfolioResponse[] = [];
    protected isLoading = true;
    protected errorMessage = '';

    ngOnInit(): void {
        this.loadPortfolios();
    }

    protected retry(): void {
        this.loadPortfolios();
    }

    protected trackPortfolioById(
        _index: number,
        portfolio: PortfolioResponse,
    ): string {
        return portfolio.id;
    }

    protected get portfolioCount(): number {
        return this.portfolios.length;
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
        return this.totalCurrentValue -
            this.totalInitialValue;
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
        PortfolioResponse | null {
        if (this.portfolios.length === 0) {
            return null;
        }

        return this.portfolios.reduce(
            (
                bestPortfolio,
                currentPortfolio,
            ) => currentPortfolio.totalReturnPercent >
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

    private loadPortfolios(): void {
        this.isLoading = true;
        this.errorMessage = '';

        this.portfolioService
            .getPortfolios({
                page: 0,
                size: 100,
                sortBy: 'createdAt',
                sortDirection: 'desc',
            })
            .pipe(
                finalize(() => {
                    this.isLoading = false;
                }),
            )
            .subscribe({
                next: (response) => {
                    this.portfolios = response.content;
                },
                error: () => {
                    this.portfolios = [];
                    this.errorMessage =
                        'Portfolio data could not be loaded. Please try again.';
                },
            });
    }
}