import {
    HttpClient,
} from '@angular/common/http';
import {
    Injectable,
    inject,
} from '@angular/core';
import {
    Observable,
    tap,
} from 'rxjs';

import {
    environment,
} from '../../../../environments/environment';
import {
    PortfolioService,
} from '../../portfolio/services/portfolio.service';
import {
    AllocationPurchaseExecuteRequest,
    AllocationPurchaseExecutionResponse,
    AllocationPurchasePreviewRequest,
    AllocationPurchasePreviewResponse,
} from '../models/allocation-purchase.models';

@Injectable({
    providedIn: 'root',
})
export class AllocationPurchaseService {
    private readonly http =
        inject(HttpClient);

    private readonly portfolioService =
        inject(PortfolioService);

    previewPurchase(
        portfolioId: string,
        request: AllocationPurchasePreviewRequest,
    ): Observable<
        AllocationPurchasePreviewResponse
    > {
        return this.http.post<
            AllocationPurchasePreviewResponse
        >(
            (
                `${this.buildAllocationPurchaseUrl(
                    portfolioId,
                )}/preview`
            ),
            request,
        );
    }

    executePurchase(
        portfolioId: string,
        request: AllocationPurchaseExecuteRequest,
    ): Observable<
        AllocationPurchaseExecutionResponse
    > {
        return this.http.post<
            AllocationPurchaseExecutionResponse
        >(
            (
                `${this.buildAllocationPurchaseUrl(
                    portfolioId,
                )}/execute`
            ),
            request,
        )
            .pipe(
                tap(() => {
                    this.portfolioService
                        .notifyPortfolioChanged({
                            portfolioId,
                            reason:
                                'transaction-created',
                        });
                }),
            );
    }

    private buildAllocationPurchaseUrl(
        portfolioId: string,
    ): string {
        return (
            `${environment.apiBaseUrl}` +
            `/portfolios/${portfolioId}` +
            '/allocation-purchases'
        );
    }
}