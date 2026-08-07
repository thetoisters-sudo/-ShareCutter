import {
    HttpErrorResponse,
} from '@angular/common/http';
import {
    ComponentFixture,
    TestBed,
} from '@angular/core/testing';
import {
    By,
} from '@angular/platform-browser';
import {
    Observable,
    of,
    throwError,
} from 'rxjs';
import {
    vi,
} from 'vitest';

import {
    AssetResponse,
} from '../../../../core/asset/models/asset.models';
import {
    AssetService,
} from '../../../../core/asset/services/asset.service';
import {
    PagedResponse,
    PortfolioResponse,
} from '../../../../core/portfolio/models/portfolio.models';
import {
    PortfolioService,
} from '../../../../core/portfolio/services/portfolio.service';
import {
    TransactionCreateRequest,
    TransactionResponse,
    TransactionSearchQuery,
    TransactionType,
    TransactionUpdateRequest,
} from '../../../../core/transaction/models/transaction.models';
import {
    TransactionService,
} from '../../../../core/transaction/services/transaction.service';
import {
    TransactionsPage,
} from './transactions-page';

class TransactionServiceStub {
    response:
        PagedResponse<TransactionResponse> = {
            content: [
                createTransaction(),
                {
                    ...createTransaction(),
                    id: 'transaction-2',
                    assetId: null,
                    transactionType: 'DEPOSIT',
                    quantity: null,
                    unitPrice: null,
                    fee: 0,
                    totalAmount: 5000,
                    notes: 'Portfolio funding',
                },
            ],
            page: 0,
            size: 10,
            totalElements: 2,
            totalPages: 1,
            first: true,
            last: true,
            hasNext: false,
            hasPrevious: false,
        };

    getTransactions = vi.fn(
        (
            _portfolioId: string,
            _query: TransactionSearchQuery,
        ): Observable<
            PagedResponse<TransactionResponse>
        > =>
            of(this.response),
    );

    createTransaction = vi.fn(
        (
            _portfolioId: string,
            _request: TransactionCreateRequest,
        ): Observable<TransactionResponse> =>
            of(createTransaction()),
    );

    updateTransaction = vi.fn(
        (
            _portfolioId: string,
            _transactionId: string,
            _request: TransactionUpdateRequest,
        ): Observable<TransactionResponse> =>
            of(createTransaction()),
    );

    deleteTransaction = vi.fn(
        (
            _portfolioId: string,
            _transactionId: string,
        ): Observable<void> =>
            of(undefined),
    );
}

class PortfolioServiceStub {
    response:
        PagedResponse<PortfolioResponse> = {
            content: [
                createPortfolio(),
                {
                    ...createPortfolio(),
                    id: 'portfolio-2',
                    name: 'Income Portfolio',
                },
            ],
            page: 0,
            size: 100,
            totalElements: 2,
            totalPages: 1,
            first: true,
            last: true,
            hasNext: false,
            hasPrevious: false,
        };

    getPortfolios = vi.fn(
        (): Observable<
            PagedResponse<PortfolioResponse>
        > =>
            of(this.response),
    );
}

class AssetServiceStub {
    response: AssetResponse[] = [
        createAsset(),
        {
            ...createAsset(),
            id: 'asset-2',
            symbol: 'MSFT',
            displayName:
                'Microsoft Corporation',
            isin: 'US5949181045',
        },
    ];

    getAssets = vi.fn(
        (
            _portfolioId: string,
        ): Observable<AssetResponse[]> =>
            of(this.response),
    );
}

describe('TransactionsPage', () => {
    let fixture:
        ComponentFixture<TransactionsPage>;

    let component:
        TransactionsPage;

    let transactionService:
        TransactionServiceStub;

    let portfolioService:
        PortfolioServiceStub;

    let assetService:
        AssetServiceStub;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                TransactionsPage,
            ],
            providers: [
                {
                    provide:
                        TransactionService,
                    useClass:
                        TransactionServiceStub,
                },
                {
                    provide:
                        PortfolioService,
                    useClass:
                        PortfolioServiceStub,
                },
                {
                    provide:
                        AssetService,
                    useClass:
                        AssetServiceStub,
                },
            ],
        }).compileComponents();

        fixture =
            TestBed.createComponent(
                TransactionsPage,
            );

        component =
            fixture.componentInstance;

        transactionService =
            TestBed.inject(
                TransactionService,
            ) as unknown as
            TransactionServiceStub;

        portfolioService =
            TestBed.inject(
                PortfolioService,
            ) as unknown as
            PortfolioServiceStub;

        assetService =
            TestBed.inject(
                AssetService,
            ) as unknown as
            AssetServiceStub;
    });

    it('should create', () => {
        fixture.detectChanges();

        expect(component).toBeTruthy();
    });

    it('should load portfolios, assets and transactions', () => {
        fixture.detectChanges();

        expect(
            portfolioService.getPortfolios,
        ).toHaveBeenCalledWith({
            page: 0,
            size: 100,
            sortBy: 'name',
            sortDirection: 'asc',
        });

        expect(
            assetService.getAssets,
        ).toHaveBeenCalledWith(
            'portfolio-1',
        );

        expect(
            transactionService
                .getTransactions,
        ).toHaveBeenCalledWith(
            'portfolio-1',
            {
                page: 0,
                size: 10,
            },
        );

        const cards =
            fixture.debugElement.queryAll(
                By.css(
                    '.transaction-card',
                ),
            );

        expect(cards).toHaveLength(2);

        expect(
            fixture.nativeElement
                .textContent,
        ).toContain('Buy');

        expect(
            fixture.nativeElement
                .textContent,
        ).toContain('Deposit');
    });

    it('should display the no-portfolio state', () => {
        portfolioService.response = {
            ...portfolioService.response,
            content: [],
            totalElements: 0,
            totalPages: 0,
        };

        fixture.detectChanges();

        expect(
            fixture.nativeElement
                .textContent,
        ).toContain(
            'No portfolios available',
        );

        expect(
            assetService.getAssets,
        ).not.toHaveBeenCalled();

        expect(
            transactionService
                .getTransactions,
        ).not.toHaveBeenCalled();
    });

    it('should display the empty transaction state', () => {
        transactionService.response = {
            ...transactionService.response,
            content: [],
            totalElements: 0,
            totalPages: 0,
        };

        fixture.detectChanges();

        expect(
            fixture.nativeElement
                .textContent,
        ).toContain(
            'No transactions found',
        );
    });

    it('should apply transaction filters', () => {
        fixture.detectChanges();

        transactionService
            .getTransactions
            .mockClear();

        const state =
            getComponentState(component);

        state.filterAssetId =
            'asset-1';

        state.filterTransactionType =
            'SELL';

        state.filterStartDate =
            '2026-07-01';

        state.filterEndDate =
            '2026-07-31';

        state.applyFilters();

        expect(
            transactionService
                .getTransactions,
        ).toHaveBeenCalledTimes(1);

        const call =
            transactionService
                .getTransactions
                .mock.calls[0];

        expect(call[0]).toBe(
            'portfolio-1',
        );

        expect(call[1].assetId).toBe(
            'asset-1',
        );

        expect(
            call[1].transactionType,
        ).toBe('SELL');

        expect(call[1].page).toBe(0);
        expect(call[1].size).toBe(10);

        expect(
            call[1].startDate,
        ).toBeTruthy();

        expect(
            call[1].endDate,
        ).toBeTruthy();
    });

    it('should reject an invalid date range', () => {
        fixture.detectChanges();

        transactionService
            .getTransactions
            .mockClear();

        const state =
            getComponentState(component);

        state.filterStartDate =
            '2026-08-01';

        state.filterEndDate =
            '2026-07-01';

        state.applyFilters();

        expect(
            transactionService
                .getTransactions,
        ).not.toHaveBeenCalled();

        expect(
            state.errorMessage,
        ).toBe(
            'Start date must not be after end date.',
        );
    });

    it('should create a buy transaction', () => {
        fixture.detectChanges();

        const state =
            getComponentState(component);

        state.openCreateDialog();

        state.formValue = {
            assetId: 'asset-1',
            transactionType: 'BUY',
            quantity: 5,
            unitPrice: 180,
            fee: 2,
            totalAmount: 902,
            currency: ' usd ',
            executedAt:
                '2026-07-30T14:00',
            notes:
                ' Initial purchase ',
        };

        state.submitCreate();

        expect(
            transactionService
                .createTransaction,
        ).toHaveBeenCalledWith(
            'portfolio-1',
            {
                assetId: 'asset-1',
                transactionType: 'BUY',
                quantity: 5,
                unitPrice: 180,
                fee: 2,
                totalAmount: 902,
                currency: 'USD',
                executedAt:
                    new Date(
                        '2026-07-30T14:00',
                    ).toISOString(),
                notes:
                    'Initial purchase',
            },
        );
    });


    it('should calculate a fractional buy quantity from total amount', () => {
        fixture.detectChanges();

        const state =
            getComponentState(component);

        state.openCreateDialog();

        state.formValue = {
            assetId: 'asset-1',
            transactionType: 'BUY',
            quantity: null,
            unitPrice: 217.35,
            fee: 2,
            totalAmount: 1000,
            currency: 'USD',
            executedAt:
                '2026-07-30T14:00',
            notes:
                'Fractional purchase by amount',
        };

        state.setCalculationMode(
            'BY_AMOUNT',
        );

        state.onTransactionTotalAmountChange();

        expect(
            state.formValue.quantity,
        ).toBe(4.60087417);

        state.submitCreate();

        expect(
            transactionService
                .createTransaction,
        ).toHaveBeenCalledWith(
            'portfolio-1',
            {
                assetId: 'asset-1',
                transactionType: 'BUY',
                quantity: 4.60087417,
                unitPrice: 217.35,
                fee: 2,
                totalAmount: 1000,
                currency: 'USD',
                executedAt:
                    new Date(
                        '2026-07-30T14:00',
                    ).toISOString(),
                notes:
                    'Fractional purchase by amount',
            },
        );
    });

    it('should calculate a fractional sell quantity from total amount', () => {
        fixture.detectChanges();

        const state =
            getComponentState(component);

        state.openCreateDialog();

        state.formValue = {
            assetId: 'asset-1',
            transactionType: 'SELL',
            quantity: null,
            unitPrice: 217.35,
            fee: 1.5,
            totalAmount: 1500,
            currency: 'USD',
            executedAt:
                '2026-07-30T14:00',
            notes:
                'Fractional sale by amount',
        };

        state.setCalculationMode(
            'BY_AMOUNT',
        );

        state.onTransactionTotalAmountChange();

        expect(
            state.formValue.quantity,
        ).toBe(6.90131125);

        state.submitCreate();

        expect(
            transactionService
                .createTransaction,
        ).toHaveBeenCalledWith(
            'portfolio-1',
            {
                assetId: 'asset-1',
                transactionType: 'SELL',
                quantity: 6.90131125,
                unitPrice: 217.35,
                fee: 1.5,
                totalAmount: 1500,
                currency: 'USD',
                executedAt:
                    new Date(
                        '2026-07-30T14:00',
                    ).toISOString(),
                notes:
                    'Fractional sale by amount',
            },
        );
    });

    it('should keep fractional quantity limited to eight decimal places', () => {
        fixture.detectChanges();

        const state =
            getComponentState(component);

        state.openCreateDialog();

        state.formValue = {
            assetId: 'asset-1',
            transactionType: 'BUY',
            quantity: null,
            unitPrice: 3,
            fee: 0,
            totalAmount: 1,
            currency: 'USD',
            executedAt:
                '2026-07-30T14:00',
            notes: '',
        };

        state.setCalculationMode(
            'BY_AMOUNT',
        );

        state.onTransactionTotalAmountChange();

        expect(
            state.formValue.quantity,
        ).toBe(0.33333333);
    });

    it('should create a deposit without an asset', () => {
        fixture.detectChanges();

        const state =
            getComponentState(component);

        state.openCreateDialog();

        state.formValue = {
            assetId: '',
            transactionType:
                'DEPOSIT',
            quantity: null,
            unitPrice: null,
            fee: 0,
            totalAmount: 5000,
            currency: 'USD',
            executedAt:
                '2026-07-30T14:00',
            notes: '',
        };

        state.submitCreate();

        expect(
            transactionService
                .createTransaction,
        ).toHaveBeenCalledWith(
            'portfolio-1',
            {
                assetId: null,
                transactionType:
                    'DEPOSIT',
                quantity: null,
                unitPrice: null,
                fee: 0,
                totalAmount: 5000,
                currency: 'USD',
                executedAt:
                    new Date(
                        '2026-07-30T14:00',
                    ).toISOString(),
                notes: null,
            },
        );
    });

    it('should reject a buy without an asset', () => {
        fixture.detectChanges();

        const state =
            getComponentState(component);

        state.openCreateDialog();

        state.formValue = {
            assetId: '',
            transactionType: 'BUY',
            quantity: 5,
            unitPrice: 180,
            fee: 0,
            totalAmount: 900,
            currency: 'USD',
            executedAt:
                '2026-07-30T14:00',
            notes: '',
        };

        state.submitCreate();

        expect(
            transactionService
                .createTransaction,
        ).not.toHaveBeenCalled();

        expect(
            state.actionErrorMessage,
        ).toBe(
            'Select an asset for this transaction type.',
        );
    });

    it('should reject an invalid currency', () => {
        fixture.detectChanges();

        const state =
            getComponentState(component);

        state.openCreateDialog();

        state.formValue = {
            assetId: '',
            transactionType:
                'DEPOSIT',
            quantity: null,
            unitPrice: null,
            fee: 0,
            totalAmount: 5000,
            currency: 'US',
            executedAt:
                '2026-07-30T14:00',
            notes: '',
        };

        state.submitCreate();

        expect(
            transactionService
                .createTransaction,
        ).not.toHaveBeenCalled();

        expect(
            state.actionErrorMessage,
        ).toBe(
            'Currency must contain exactly 3 letters.',
        );
    });

    it('should edit a transaction', () => {
        fixture.detectChanges();

        const transaction =
            createTransaction();

        const state =
            getComponentState(component);

        state.openEditDialog(
            transaction,
        );

        state.formValue.quantity = 3;
        state.formValue.totalAmount = 542;
        state.formValue.notes =
            'Updated purchase';

        state.submitEdit();

        expect(
            transactionService
                .updateTransaction,
        ).toHaveBeenCalledWith(
            'portfolio-1',
            transaction.id,
            expect.objectContaining({
                assetId: 'asset-1',
                transactionType: 'BUY',
                quantity: 3,
                unitPrice: 180,
                fee: 2,
                totalAmount: 542,
                currency: 'USD',
                notes:
                    'Updated purchase',
            }),
        );
    });

    it('should delete a transaction', () => {
        fixture.detectChanges();

        const transaction =
            createTransaction();

        const state =
            getComponentState(component);

        state.openDeleteDialog(
            transaction,
        );

        state.submitDelete();

        expect(
            transactionService
                .deleteTransaction,
        ).toHaveBeenCalledWith(
            'portfolio-1',
            transaction.id,
        );
    });

    it('should stop submitting after a server error', () => {
        transactionService
            .createTransaction
            .mockReturnValueOnce(
                throwError(
                    () =>
                        new HttpErrorResponse({
                            status: 500,
                            statusText:
                                'Server Error',
                        }),
                ),
            );

        fixture.detectChanges();

        const state =
            getComponentState(component);

        state.openCreateDialog();

        state.formValue = {
            assetId: '',
            transactionType:
                'DEPOSIT',
            quantity: null,
            unitPrice: null,
            fee: 0,
            totalAmount: 5000,
            currency: 'USD',
            executedAt:
                '2026-07-30T14:00',
            notes: '',
        };

        state.submitCreate();

        expect(
            state.isSubmitting,
        ).toBe(false);

        expect(
            state.actionErrorMessage,
        ).toBe(
            'The transaction could not be created.',
        );
    });

    it('should display a transaction load error', () => {
        transactionService
            .getTransactions
            .mockReturnValueOnce(
                throwError(
                    () =>
                        new HttpErrorResponse({
                            status: 500,
                            statusText:
                                'Server Error',
                        }),
                ),
            );

        fixture.detectChanges();

        expect(
            fixture.nativeElement
                .textContent,
        ).toContain(
            'Transactions unavailable',
        );

        expect(
            getComponentState(component)
                .isLoadingTransactions,
        ).toBe(false);
    });
});

interface TransactionsPageTestState {
    filterAssetId: string;

    calculationMode:
    'BY_QUANTITY' |
    'BY_AMOUNT';

    filterTransactionType:
    TransactionType | '';

    filterStartDate: string;
    filterEndDate: string;

    isLoadingTransactions: boolean;
    isSubmitting: boolean;

    errorMessage: string;
    actionErrorMessage: string;

    formValue: {
        assetId: string;
        transactionType:
        TransactionType;
        quantity: number | null;
        unitPrice: number | null;
        fee: number | null;
        totalAmount: number | null;
        currency: string;
        executedAt: string;
        notes: string;
    };

    applyFilters(): void;

    openCreateDialog(): void;

    setCalculationMode(
        mode:
            'BY_QUANTITY' |
            'BY_AMOUNT',
    ): void;

    onTransactionTotalAmountChange():
        void;

    submitCreate(): void;

    openEditDialog(
        transaction:
            TransactionResponse,
    ): void;

    submitEdit(): void;

    openDeleteDialog(
        transaction:
            TransactionResponse,
    ): void;

    submitDelete(): void;
}

function getComponentState(
    component: TransactionsPage,
): TransactionsPageTestState {
    return component as unknown as
        TransactionsPageTestState;
}

function createPortfolio():
    PortfolioResponse {
    return {
        id: 'portfolio-1',
        userId: 'user-1',
        name: 'Growth Portfolio',
        creationMethod: 'BY_AMOUNT',
        initialValue: 10000,
        currentValue: 11500,
        totalRealizedProfit: 500,
        totalUnrealizedProfit: 1000,
        totalReturnPercent: 15,
        createdAt:
            '2026-07-20T10:00:00Z',
        updatedAt:
            '2026-07-29T10:00:00Z',
    };
}

function createAsset():
    AssetResponse {
    return {
        id: 'asset-1',
        portfolioId: 'portfolio-1',
        symbol: 'AAPL',
        displayName: 'Apple Inc.',
        assetType: 'STOCK',
        currency: 'USD',
        isin: 'US0378331005',
        exchange: 'NASDAQ',
        notes:
            'Core technology holding',
        createdAt:
            '2026-07-30T10:00:00Z',
        updatedAt:
            '2026-07-30T10:00:00Z',
    };
}

function createTransaction():
    TransactionResponse {
    return {
        id: 'transaction-1',
        portfolioId: 'portfolio-1',
        assetId: 'asset-1',
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
}