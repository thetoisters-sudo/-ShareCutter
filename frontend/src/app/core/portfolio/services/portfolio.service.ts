import {
    HttpClient,
    HttpParams,
} from '@angular/common/http';
import {
    Injectable,
    inject,
} from '@angular/core';
import {
    Observable,
    Subject,
} from 'rxjs';

import {
    environment,
} from '../../../../environments/environment';
import {
    PagedResponse,
    PortfolioAllocationResponse,
    PortfolioCreateRequest,
    PortfolioListQuery,
    PortfolioRenameRequest,
    PortfolioResponse,
    PortfolioSummaryResponse,
    PortfolioValueUpdateRequest,
} from '../models/portfolio.models';

export interface PortfolioChangeEvent {
    portfolioId: string | null;
    reason:
    | 'created'
    | 'renamed'
    | 'deleted'
    | 'value-updated'
    | 'asset-created'
    | 'asset-updated'
    | 'asset-deleted'
    | 'transaction-created'
    | 'transaction-updated'
    | 'transaction-deleted'
    | 'target-weight-updated'
    | 'allocation-executed'
    | 'market-price-updated'
    | 'rebuilt';
}

export interface TargetWeightUpdateRequest {
    targetWeightPercent: number;
}

@Injectable({
    providedIn: 'root',
})
export class PortfolioService {
    private readonly http =
        inject(HttpClient);

    private readonly portfoliosUrl =
        `${environment.apiBaseUrl}/portfolios`;

    private readonly portfolioChangedSubject =
        new Subject<PortfolioChangeEvent>();

    readonly portfolioChanged$ =
        this.portfolioChangedSubject.asObservable();

    getPortfolios(
        query: PortfolioListQuery = {},
    ): Observable<
        PagedResponse<PortfolioResponse>
    > {
        return this.http.get<
            PagedResponse<PortfolioResponse>
        >(
            this.portfoliosUrl,
            {
                params:
                    this.buildListParams(query),
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
        return this.http.get<
            PortfolioSummaryResponse
        >(
            `${this.portfoliosUrl}/${portfolioId}/analytics/summary`,
        );
    }

    getPortfolioAllocation(
        portfolioId: string,
    ): Observable<
        PortfolioAllocationResponse
    > {
        return this.http.get<
            PortfolioAllocationResponse
        >(
            `${this.portfoliosUrl}/${portfolioId}/analytics/allocation`,
        );
    }

    updateTargetWeight(
        portfolioId: string,
        assetId: string,
        request: TargetWeightUpdateRequest,
    ): Observable<
        PortfolioAllocationResponse
    > {
        return this.http.patch<
            PortfolioAllocationResponse
        >(
            (
                `${this.portfoliosUrl}/${portfolioId}` +
                `/analytics/allocation/${assetId}` +
                '/target-weight'
            ),
            request,
        );
    }

    rebuildPortfolioState(
        portfolioId: string,
    ): Observable<void> {
        return this.http.post<void>(
            (
                `${this.portfoliosUrl}/${portfolioId}` +
                '/transactions/rebuild'
            ),
            null,
        );
    }

    notifyPortfolioChanged(
        event: PortfolioChangeEvent,
    ): void {
        this.portfolioChangedSubject.next(
            event,
        );
    }

    private buildListParams(
        query: PortfolioListQuery,
    ): HttpParams {
        let params =
            new HttpParams();

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

        if (
            query.sortDirection !==
            undefined
        ) {
            params = params.set(
                'sortDirection',
                query.sortDirection,
            );
        }

        return params;
    }
}