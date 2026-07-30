import {
    provideHttpClient,
} from '@angular/common/http';
import {
    HttpTestingController,
    provideHttpClientTesting,
} from '@angular/common/http/testing';
import {
    TestBed,
} from '@angular/core/testing';

import {
    environment,
} from '../../../../environments/environment';
import {
    PagedResponse,
} from '../../portfolio/models/portfolio.models';
import {
    TransactionCreateRequest,
    TransactionResponse,
    TransactionUpdateRequest,
} from '../models/transaction.models';
import {
    TransactionService,
} from './transaction.service';

describe('TransactionService', () => {
    let service:
        TransactionService;

    let httpTestingController:
        HttpTestingController;

    const portfolioId =
        'portfolio-1';

    const transactionId =
        'transaction-1';

    const assetId =
        'asset-1';

    const transactionsUrl =
        `${environment.apiBaseUrl}` +
        `/portfolios/${portfolioId}` +
        '/transactions';

    const transaction:
        TransactionResponse = {
        id: transactionId,
        portfolioId,
        assetId,
        transactionType: 'BUY',
        quantity: 5,
        unitPrice: 180,
        fee: 2,
        totalAmount: 902,
        currency: 'USD',
        executedAt:
            '2026-07-30T10:00:00Z',
        notes:
            'Initial Apple purchase',
        createdAt:
            '2026-07-30T10:05:00Z',
        updatedAt:
            '2026-07-30T10:05:00Z',
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                TransactionService,
                provideHttpClient(),
                provideHttpClientTesting(),
            ],
        });

        service =
            TestBed.inject(
                TransactionService,
            );

        httpTestingController =
            TestBed.inject(
                HttpTestingController,
            );
    });

    afterEach(() => {
        httpTestingController.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should get paged transactions for a portfolio', () => {
        const response:
            PagedResponse<TransactionResponse> = {
            content: [
                transaction,
            ],
            page: 0,
            size: 20,
            totalElements: 1,
            totalPages: 1,
            first: true,
            last: true,
            hasNext: false,
            hasPrevious: false,
        };

        let actualResponse:
            PagedResponse<TransactionResponse>
            | undefined;

        service
            .getTransactions(
                portfolioId,
            )
            .subscribe(
                (result) => {
                    actualResponse =
                        result;
                },
            );

        const request =
            httpTestingController.expectOne(
                (candidate) =>
                    candidate.url ===
                    transactionsUrl,
            );

        expect(
            request.request.method,
        ).toBe('GET');

        expect(
            request.request.params.keys(),
        ).toEqual([]);

        request.flush(response);

        expect(actualResponse).toEqual(
            response,
        );
    });

    it('should send transaction search parameters', () => {
        service
            .getTransactions(
                portfolioId,
                {
                    assetId,
                    transactionType:
                        'SELL',
                    startDate:
                        '2026-01-01T00:00:00Z',
                    endDate:
                        '2026-12-31T23:59:59Z',
                    page: 2,
                    size: 50,
                },
            )
            .subscribe();

        const request =
            httpTestingController.expectOne(
                (candidate) =>
                    candidate.url ===
                    transactionsUrl,
            );

        expect(
            request.request.method,
        ).toBe('GET');

        expect(
            request.request.params.get(
                'assetId',
            ),
        ).toBe(assetId);

        expect(
            request.request.params.get(
                'type',
            ),
        ).toBe('SELL');

        expect(
            request.request.params.get(
                'startDate',
            ),
        ).toBe(
            '2026-01-01T00:00:00Z',
        );

        expect(
            request.request.params.get(
                'endDate',
            ),
        ).toBe(
            '2026-12-31T23:59:59Z',
        );

        expect(
            request.request.params.get(
                'page',
            ),
        ).toBe('2');

        expect(
            request.request.params.get(
                'size',
            ),
        ).toBe('50');

        request.flush({
            content: [],
            page: 2,
            size: 50,
            totalElements: 0,
            totalPages: 0,
            first: false,
            last: true,
            hasNext: false,
            hasPrevious: true,
        });
    });

    it('should get a transaction by id', () => {
        let actualResponse:
            TransactionResponse
            | undefined;

        service
            .getTransaction(
                portfolioId,
                transactionId,
            )
            .subscribe(
                (result) => {
                    actualResponse =
                        result;
                },
            );

        const request =
            httpTestingController.expectOne(
                `${transactionsUrl}/${transactionId}`,
            );

        expect(
            request.request.method,
        ).toBe('GET');

        request.flush(transaction);

        expect(actualResponse).toEqual(
            transaction,
        );
    });

    it('should create a transaction', () => {
        const createRequest:
            TransactionCreateRequest = {
            assetId,
            transactionType: 'BUY',
            quantity: 5,
            unitPrice: 180,
            fee: 2,
            totalAmount: 902,
            currency: 'USD',
            executedAt:
                '2026-07-30T10:00:00Z',
            notes:
                'Initial Apple purchase',
        };

        let actualResponse:
            TransactionResponse
            | undefined;

        service
            .createTransaction(
                portfolioId,
                createRequest,
            )
            .subscribe(
                (result) => {
                    actualResponse =
                        result;
                },
            );

        const request =
            httpTestingController.expectOne(
                transactionsUrl,
            );

        expect(
            request.request.method,
        ).toBe('POST');

        expect(
            request.request.body,
        ).toEqual(
            createRequest,
        );

        request.flush(transaction);

        expect(actualResponse).toEqual(
            transaction,
        );
    });

    it('should create a transaction with nullable optional fields', () => {
        const createRequest:
            TransactionCreateRequest = {
            assetId: null,
            transactionType:
                'DEPOSIT',
            quantity: null,
            unitPrice: null,
            fee: null,
            totalAmount: 5000,
            currency: 'USD',
            executedAt:
                '2026-07-30T11:00:00Z',
            notes: null,
        };

        service
            .createTransaction(
                portfolioId,
                createRequest,
            )
            .subscribe();

        const request =
            httpTestingController.expectOne(
                transactionsUrl,
            );

        expect(
            request.request.method,
        ).toBe('POST');

        expect(
            request.request.body,
        ).toEqual(
            createRequest,
        );

        request.flush({
            ...transaction,
            ...createRequest,
            id: 'transaction-2',
        });
    });

    it('should update a transaction', () => {
        const updateRequest:
            TransactionUpdateRequest = {
            assetId,
            transactionType:
                'SELL',
            quantity: 2,
            unitPrice: 195,
            fee: 1.5,
            totalAmount: 388.5,
            currency: 'USD',
            executedAt:
                '2026-07-30T12:00:00Z',
            notes:
                'Partial position sale',
        };

        let actualResponse:
            TransactionResponse
            | undefined;

        service
            .updateTransaction(
                portfolioId,
                transactionId,
                updateRequest,
            )
            .subscribe(
                (result) => {
                    actualResponse =
                        result;
                },
            );

        const request =
            httpTestingController.expectOne(
                `${transactionsUrl}/${transactionId}`,
            );

        expect(
            request.request.method,
        ).toBe('PUT');

        expect(
            request.request.body,
        ).toEqual(
            updateRequest,
        );

        const updatedTransaction:
            TransactionResponse = {
            ...transaction,
            ...updateRequest,
            updatedAt:
                '2026-07-30T12:05:00Z',
        };

        request.flush(
            updatedTransaction,
        );

        expect(actualResponse).toEqual(
            updatedTransaction,
        );
    });

    it('should delete a transaction', () => {
        let completed =
            false;

        service
            .deleteTransaction(
                portfolioId,
                transactionId,
            )
            .subscribe({
                complete: () => {
                    completed = true;
                },
            });

        const request =
            httpTestingController.expectOne(
                `${transactionsUrl}/${transactionId}`,
            );

        expect(
            request.request.method,
        ).toBe('DELETE');

        request.flush(null);

        expect(completed).toBe(true);
    });
});