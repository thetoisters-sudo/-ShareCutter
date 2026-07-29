import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
    PagedResponse,
    PortfolioCreateRequest,
    PortfolioListQuery,
    PortfolioRenameRequest,
    PortfolioResponse,
    PortfolioSummaryResponse,
    PortfolioValueUpdateRequest,
} from '../models/portfolio.models';

@Injectable({
    providedIn: 'root',
})
export class PortfolioService {
    private readonly http = inject(HttpClient);

    private readonly portfoliosUrl =
        `${environment.apiBaseUrl}/portfolios`;

    getPortfolios(
        query: PortfolioListQuery = {},
    ): Observable<PagedResponse<PortfolioResponse>> {
        const params = this.buildListParams(query);

        return this.http.get<
            PagedResponse<PortfolioResponse>
        >(
            this.portfoliosUrl,
            {
                params,
            },
        );
    }

    getPortfolio(
        portfolioId: string,
    ): Observable<PortfolioResponse> {
        return this.http.get<PortfolioResponse>(
            `${this.portfoliosUrl}/${portfolioId}`,
        );
    }

    createPortfolio(
        request: PortfolioCreateRequest,
    ): Observable<PortfolioResponse> {
        return this.http.post<PortfolioResponse>(
            this.portfoliosUrl,
            request,
        );
    }

    renamePortfolio(
        portfolioId: string,
        request: PortfolioRenameRequest,
    ): Observable<PortfolioResponse> {
        return this.http.patch<PortfolioResponse>(
            `${this.portfoliosUrl}/${portfolioId}/name`,
            request,
        );
    }

    updatePortfolioValue(
        portfolioId: string,
        request: PortfolioValueUpdateRequest,
    ): Observable<PortfolioResponse> {
        return this.http.patch<PortfolioResponse>(
            `${this.portfoliosUrl}/${portfolioId}/value`,
            request,
        );
    }

    deletePortfolio(
        portfolioId: string,
    ): Observable<void> {
        return this.http.delete<void>(
            `${this.portfoliosUrl}/${portfolioId}`,
        );
    }

    getPortfolioSummary(
        portfolioId: string,
    ): Observable<PortfolioSummaryResponse> {
        return this.http.get<PortfolioSummaryResponse>(
            `${this.portfoliosUrl}/${portfolioId}/analytics/summary`,
        );
    }

    private buildListParams(
        query: PortfolioListQuery,
    ): HttpParams {
        let params = new HttpParams();

        if (query.page !== undefined) {
            params = params.set(
                'page',
                query.page.toString(),
            );
        }

        if (query.size !== undefined) {
            params = params.set(
                'size',
                query.size.toString(),
            );
        }

        if (query.sortBy !== undefined) {
            params = params.set(
                'sortBy',
                query.sortBy,
            );
        }

        if (query.sortDirection !== undefined) {
            params = params.set(
                'sortDirection',
                query.sortDirection,
            );
        }

        return params;
    }
}