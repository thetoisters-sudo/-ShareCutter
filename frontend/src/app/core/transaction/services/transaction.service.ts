import {
    HttpClient,
    HttpParams,
} from '@angular/common/http';
import {
    Injectable,
    inject,
} from '@angular/core';
import { Observable } from 'rxjs';

import {
    environment,
} from '../../../../environments/environment';
import {
    PagedResponse,
} from '../../portfolio/models/portfolio.models';
import {
    TransactionCreateRequest,
    TransactionResponse,
    TransactionSearchQuery,
    TransactionUpdateRequest,
} from '../models/transaction.models';

@Injectable({
    providedIn: 'root',
})
export class TransactionService {
    private readonly http =
        inject(HttpClient);

    getTransactions(
        portfolioId: string,
        query: TransactionSearchQuery = {},
    ): Observable<
        PagedResponse<TransactionResponse>
    > {
        const params =
            this.buildSearchParams(query);

        return this.http.get<
            PagedResponse<TransactionResponse>
        >(
            this.buildTransactionsUrl(
                portfolioId,
            ),
            {
                params,
            },
        );
    }

    getTransaction(
        portfolioId: string,
        transactionId: string,
    ): Observable<TransactionResponse> {
        return this.http.get<
            TransactionResponse
        >(
            `${this.buildTransactionsUrl(
                portfolioId,
            )
            }/${transactionId}`,
        );
    }

    createTransaction(
        portfolioId: string,
        request: TransactionCreateRequest,
    ): Observable<TransactionResponse> {
        return this.http.post<
            TransactionResponse
        >(
            this.buildTransactionsUrl(
                portfolioId,
            ),
            request,
        );
    }

    updateTransaction(
        portfolioId: string,
        transactionId: string,
        request: TransactionUpdateRequest,
    ): Observable<TransactionResponse> {
        return this.http.put<
            TransactionResponse
        >(
            `${this.buildTransactionsUrl(
                portfolioId,
            )
            }/${transactionId}`,
            request,
        );
    }

    deleteTransaction(
        portfolioId: string,
        transactionId: string,
    ): Observable<void> {
        return this.http.delete<void>(
            `${this.buildTransactionsUrl(
                portfolioId,
            )
            }/${transactionId}`,
        );
    }

    private buildTransactionsUrl(
        portfolioId: string,
    ): string {
        return (
            `${environment.apiBaseUrl}` +
            `/portfolios/${portfolioId}` +
            '/transactions'
        );
    }

    private buildSearchParams(
        query: TransactionSearchQuery,
    ): HttpParams {
        let params =
            new HttpParams();

        if (query.assetId !== undefined) {
            params = params.set(
                'assetId',
                query.assetId,
            );
        }

        if (
            query.transactionType !==
            undefined
        ) {
            params = params.set(
                'type',
                query.transactionType,
            );
        }

        if (
            query.startDate !== undefined
        ) {
            params = params.set(
                'startDate',
                query.startDate,
            );
        }

        if (
            query.endDate !== undefined
        ) {
            params = params.set(
                'endDate',
                query.endDate,
            );
        }

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

        return params;
    }
}