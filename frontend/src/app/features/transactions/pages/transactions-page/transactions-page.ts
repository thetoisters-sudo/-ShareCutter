import {
    DatePipe,
    DecimalPipe,
} from '@angular/common';
import {
    HttpErrorResponse,
} from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnInit,
    inject,
} from '@angular/core';
import {
    FormsModule,
} from '@angular/forms';

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
    TRANSACTION_TYPE_OPTIONS,
    TransactionCreateRequest,
    TransactionResponse,
    TransactionSearchQuery,
    TransactionType,
    TransactionUpdateRequest,
} from '../../../../core/transaction/models/transaction.models';
import {
    TransactionService,
} from '../../../../core/transaction/services/transaction.service';

type TransactionDialogMode =
    | 'create'
    | 'edit'
    | 'delete'
    | null;

type TransactionAction =
    | 'create'
    | 'edit'
    | 'delete';

interface TransactionFormValue {
    assetId: string;
    transactionType: TransactionType;
    quantity: number | null;
    unitPrice: number | null;
    fee: number | null;
    totalAmount: number | null;
    currency: string;
    executedAt: string;
    notes: string;
}

@Component({
    selector: 'app-transactions-page',
    imports: [
        DatePipe,
        DecimalPipe,
        FormsModule,
    ],
    templateUrl: './transactions-page.html',
    styleUrl: './transactions-page.scss',
    changeDetection:
        ChangeDetectionStrategy.OnPush,
})
export class TransactionsPage implements OnInit {
    private readonly transactionService =
        inject(TransactionService);

    private readonly portfolioService =
        inject(PortfolioService);

    private readonly assetService =
        inject(AssetService);

    private readonly changeDetectorRef =
        inject(ChangeDetectorRef);

    protected readonly transactionTypeOptions =
        TRANSACTION_TYPE_OPTIONS;

    protected portfolios: PortfolioResponse[] = [];
    protected assets: AssetResponse[] = [];
    protected transactions: TransactionResponse[] = [];

    protected selectedPortfolioId = '';

    protected filterAssetId = '';
    protected filterTransactionType:
        TransactionType | '' = '';
    protected filterStartDate = '';
    protected filterEndDate = '';

    protected currentPage = 0;
    protected pageSize = 10;
    protected totalElements = 0;
    protected totalPages = 0;
    protected isFirstPage = true;
    protected isLastPage = true;

    protected isLoadingPortfolios = true;
    protected isLoadingTransactions = false;
    protected isLoadingAssets = false;
    protected isSubmitting = false;

    protected errorMessage = '';
    protected actionErrorMessage = '';
    protected successMessage = '';

    protected dialogMode:
        TransactionDialogMode = null;

    protected selectedTransaction:
        TransactionResponse | null = null;

    protected formValue:
        TransactionFormValue =
        this.createEmptyFormValue();

    ngOnInit(): void {
        this.loadPortfolios();
    }

    protected retry(): void {
        if (this.portfolios.length === 0) {
            this.loadPortfolios();
            return;
        }

        this.loadPortfolioData();
    }

    protected selectPortfolio(
        portfolioId: string,
    ): void {
        if (
            portfolioId ===
            this.selectedPortfolioId ||
            this.isLoadingTransactions
        ) {
            return;
        }

        this.selectedPortfolioId =
            portfolioId;

        this.resetFilters();
        this.currentPage = 0;
        this.loadPortfolioData();
    }

    protected applyFilters(): void {
        if (
            this.filterStartDate &&
            this.filterEndDate &&
            this.filterStartDate >
            this.filterEndDate
        ) {
            this.errorMessage =
                'Start date must not be after end date.';
            return;
        }

        this.currentPage = 0;
        this.loadTransactions();
    }

    protected clearFilters(): void {
        this.resetFilters();
        this.currentPage = 0;
        this.loadTransactions();
    }

    protected goToPreviousPage(): void {
        if (
            this.isFirstPage ||
            this.isLoadingTransactions
        ) {
            return;
        }

        this.currentPage -= 1;
        this.loadTransactions();
    }

    protected goToNextPage(): void {
        if (
            this.isLastPage ||
            this.isLoadingTransactions
        ) {
            return;
        }

        this.currentPage += 1;
        this.loadTransactions();
    }

    protected openCreateDialog(): void {
        if (!this.selectedPortfolioId) {
            return;
        }

        this.clearMessages();
        this.selectedTransaction = null;
        this.formValue =
            this.createEmptyFormValue();
        this.dialogMode = 'create';
    }

    protected openEditDialog(
        transaction: TransactionResponse,
    ): void {
        this.clearMessages();
        this.selectedTransaction =
            transaction;

        this.formValue = {
            assetId:
                transaction.assetId ?? '',
            transactionType:
                transaction.transactionType,
            quantity:
                transaction.quantity,
            unitPrice:
                transaction.unitPrice,
            fee:
                transaction.fee,
            totalAmount:
                transaction.totalAmount,
            currency:
                transaction.currency,
            executedAt:
                this.toDateTimeLocalValue(
                    transaction.executedAt,
                ),
            notes:
                transaction.notes ?? '',
        };

        this.dialogMode = 'edit';
    }

    protected openDeleteDialog(
        transaction: TransactionResponse,
    ): void {
        this.clearMessages();
        this.selectedTransaction =
            transaction;
        this.dialogMode = 'delete';
    }

    protected closeDialog(): void {
        if (this.isSubmitting) {
            return;
        }

        this.dialogMode = null;
        this.selectedTransaction = null;
        this.actionErrorMessage = '';
    }

    protected submitCreate(): void {
        const request =
            this.buildRequest();

        if (
            !request ||
            !this.selectedPortfolioId
        ) {
            return;
        }

        this.startSubmission();

        this.transactionService
            .createTransaction(
                this.selectedPortfolioId,
                request,
            )
            .subscribe({
                next: () => {
                    this.finishSubmission();
                    this.closeCompletedDialog();
                    this.currentPage = 0;
                    this.successMessage =
                        'Transaction created successfully.';
                    this.changeDetectorRef
                        .markForCheck();
                    this.loadTransactions(false);
                },
                error: (error: unknown) => {
                    this.finishSubmission();
                    this.actionErrorMessage =
                        this.resolveActionError(
                            error,
                            'create',
                        );
                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    protected submitEdit(): void {
        const transaction =
            this.selectedTransaction;

        const request =
            this.buildRequest();

        if (
            !transaction ||
            !request ||
            !this.selectedPortfolioId
        ) {
            return;
        }

        this.startSubmission();

        this.transactionService
            .updateTransaction(
                this.selectedPortfolioId,
                transaction.id,
                request,
            )
            .subscribe({
                next: () => {
                    this.finishSubmission();
                    this.closeCompletedDialog();
                    this.successMessage =
                        'Transaction updated successfully.';
                    this.changeDetectorRef
                        .markForCheck();
                    this.loadTransactions(false);
                },
                error: (error: unknown) => {
                    this.finishSubmission();
                    this.actionErrorMessage =
                        this.resolveActionError(
                            error,
                            'edit',
                        );
                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    protected submitDelete(): void {
        const transaction =
            this.selectedTransaction;

        if (
            !transaction ||
            !this.selectedPortfolioId
        ) {
            return;
        }

        this.startSubmission();

        this.transactionService
            .deleteTransaction(
                this.selectedPortfolioId,
                transaction.id,
            )
            .subscribe({
                next: () => {
                    this.finishSubmission();
                    this.closeCompletedDialog();
                    this.successMessage =
                        'Transaction deleted successfully.';

                    if (
                        this.transactions.length === 1 &&
                        this.currentPage > 0
                    ) {
                        this.currentPage -= 1;
                    }

                    this.changeDetectorRef
                        .markForCheck();
                    this.loadTransactions(false);
                },
                error: (error: unknown) => {
                    this.finishSubmission();
                    this.actionErrorMessage =
                        this.resolveActionError(
                            error,
                            'delete',
                        );
                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    protected dismissSuccessMessage(): void {
        this.successMessage = '';
    }

    protected onTransactionTypeChange(): void {
        if (
            !this.transactionRequiresAsset(
                this.formValue.transactionType,
            )
        ) {
            this.formValue.assetId = '';
            this.formValue.quantity = null;
            this.formValue.unitPrice = null;
        }

        if (
            this.formValue.transactionType !==
            'BUY' &&
            this.formValue.transactionType !==
            'SELL'
        ) {
            this.formValue.quantity = null;
            this.formValue.unitPrice = null;
        }
    }

    protected transactionRequiresAsset(
        type: TransactionType,
    ): boolean {
        return (
            type === 'BUY' ||
            type === 'SELL' ||
            type === 'DIVIDEND'
        );
    }

    protected transactionUsesQuantity(
        type: TransactionType,
    ): boolean {
        return (
            type === 'BUY' ||
            type === 'SELL'
        );
    }

    protected get selectedPortfolio():
        PortfolioResponse | null {
        return (
            this.portfolios.find(
                (portfolio) =>
                    portfolio.id ===
                    this.selectedPortfolioId,
            ) ?? null
        );
    }

    protected get visiblePageNumber(): number {
        if (this.totalPages === 0) {
            return 0;
        }

        return this.currentPage + 1;
    }

    protected get totalDisplayedAmount(): number {
        return this.transactions.reduce(
            (
                total,
                transaction,
            ) =>
                total +
                transaction.totalAmount,
            0,
        );
    }

    protected get displayedTransactionTypes():
        number {
        return new Set(
            this.transactions.map(
                (transaction) =>
                    transaction.transactionType,
            ),
        ).size;
    }

    protected getTransactionTypeLabel(
        type: TransactionType,
    ): string {
        return (
            this.transactionTypeOptions.find(
                (option) =>
                    option.value === type,
            )?.label ?? type
        );
    }

    protected getAssetLabel(
        assetId: string | null,
    ): string {
        if (!assetId) {
            return 'No asset';
        }

        const asset =
            this.assets.find(
                (candidate) =>
                    candidate.id === assetId,
            );

        if (!asset) {
            return 'Unknown asset';
        }

        return (
            `${asset.symbol} · ` +
            asset.displayName
        );
    }

    private loadPortfolios(): void {
        this.isLoadingPortfolios = true;
        this.errorMessage = '';
        this.successMessage = '';
        this.changeDetectorRef.markForCheck();

        this.portfolioService
            .getPortfolios({
                page: 0,
                size: 100,
                sortBy: 'name',
                sortDirection: 'asc',
            })
            .subscribe({
                next: (
                    response:
                        PagedResponse<PortfolioResponse>,
                ) => {
                    this.portfolios =
                        response.content;

                    this.isLoadingPortfolios =
                        false;

                    if (
                        this.portfolios.length > 0
                    ) {
                        this.selectedPortfolioId =
                            this.portfolios[0].id;

                        this.loadPortfolioData();
                    } else {
                        this.resetPortfolioData();
                    }

                    this.changeDetectorRef
                        .markForCheck();
                },
                error: (error: unknown) => {
                    this.isLoadingPortfolios =
                        false;

                    this.portfolios = [];
                    this.resetPortfolioData();

                    this.errorMessage =
                        this.resolvePortfolioLoadError(
                            error,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    private loadPortfolioData(): void {
        if (!this.selectedPortfolioId) {
            return;
        }

        this.loadAssets();
        this.loadTransactions();
    }

    private loadAssets(): void {
        if (!this.selectedPortfolioId) {
            return;
        }

        this.isLoadingAssets = true;
        this.changeDetectorRef.markForCheck();

        this.assetService
            .getAssets(
                this.selectedPortfolioId,
            )
            .subscribe({
                next: (
                    assets: AssetResponse[],
                ) => {
                    this.assets = assets;
                    this.isLoadingAssets = false;
                    this.changeDetectorRef
                        .markForCheck();
                },
                error: () => {
                    this.assets = [];
                    this.isLoadingAssets = false;
                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    private loadTransactions(
        clearMessages = true,
    ): void {
        if (!this.selectedPortfolioId) {
            return;
        }

        this.isLoadingTransactions = true;
        this.errorMessage = '';

        if (clearMessages) {
            this.successMessage = '';
        }

        this.changeDetectorRef.markForCheck();

        const query =
            this.buildSearchQuery();

        this.transactionService
            .getTransactions(
                this.selectedPortfolioId,
                query,
            )
            .subscribe({
                next: (
                    response:
                        PagedResponse<TransactionResponse>,
                ) => {
                    this.applyResponse(response);
                    this.isLoadingTransactions =
                        false;
                    this.changeDetectorRef
                        .markForCheck();
                },
                error: (error: unknown) => {
                    this.isLoadingTransactions =
                        false;
                    this.transactions = [];
                    this.totalElements = 0;
                    this.totalPages = 0;
                    this.isFirstPage = true;
                    this.isLastPage = true;

                    this.errorMessage =
                        this.resolveTransactionLoadError(
                            error,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    private buildSearchQuery():
        TransactionSearchQuery {
        const query:
            TransactionSearchQuery = {
            page: this.currentPage,
            size: this.pageSize,
        };

        if (this.filterAssetId) {
            query.assetId =
                this.filterAssetId;
        }

        if (this.filterTransactionType) {
            query.transactionType =
                this.filterTransactionType;
        }

        if (this.filterStartDate) {
            query.startDate =
                this.startOfLocalDayToIso(
                    this.filterStartDate,
                );
        }

        if (this.filterEndDate) {
            query.endDate =
                this.endOfLocalDayToIso(
                    this.filterEndDate,
                );
        }

        return query;
    }

    private buildRequest():
        TransactionCreateRequest
        | TransactionUpdateRequest
        | null {
        const type =
            this.formValue.transactionType;

        const currency =
            this.formValue.currency
                .trim()
                .toUpperCase();

        const notes =
            this.normalizeOptionalText(
                this.formValue.notes,
            );

        const requiresAsset =
            this.transactionRequiresAsset(type);

        const usesQuantity =
            this.transactionUsesQuantity(type);

        if (
            requiresAsset &&
            !this.formValue.assetId
        ) {
            this.actionErrorMessage =
                'Select an asset for this transaction type.';
            return null;
        }

        if (
            usesQuantity &&
            (
                this.formValue.quantity ===
                null ||
                this.formValue.quantity <= 0
            )
        ) {
            this.actionErrorMessage =
                'Quantity must be greater than zero.';
            return null;
        }

        if (
            usesQuantity &&
            (
                this.formValue.unitPrice ===
                null ||
                this.formValue.unitPrice < 0
            )
        ) {
            this.actionErrorMessage =
                'Unit price must be zero or greater.';
            return null;
        }

        if (
            this.formValue.fee !== null &&
            this.formValue.fee < 0
        ) {
            this.actionErrorMessage =
                'Fee must be zero or greater.';
            return null;
        }

        if (
            this.formValue.totalAmount ===
            null ||
            this.formValue.totalAmount < 0
        ) {
            this.actionErrorMessage =
                'Total amount must be zero or greater.';
            return null;
        }

        if (!/^[A-Z]{3}$/.test(currency)) {
            this.actionErrorMessage =
                'Currency must contain exactly 3 letters.';
            return null;
        }

        if (!this.formValue.executedAt) {
            this.actionErrorMessage =
                'Execution date and time are required.';
            return null;
        }

        if (
            notes !== null &&
            notes.length > 2000
        ) {
            this.actionErrorMessage =
                'Notes must not exceed 2000 characters.';
            return null;
        }

        const executedAt =
            new Date(
                this.formValue.executedAt,
            );

        if (
            Number.isNaN(
                executedAt.getTime(),
            )
        ) {
            this.actionErrorMessage =
                'Execution date and time are invalid.';
            return null;
        }

        return {
            assetId:
                requiresAsset
                    ? this.formValue.assetId
                    : null,
            transactionType: type,
            quantity:
                usesQuantity
                    ? this.formValue.quantity
                    : null,
            unitPrice:
                usesQuantity
                    ? this.formValue.unitPrice
                    : null,
            fee:
                this.formValue.fee,
            totalAmount:
                this.formValue.totalAmount,
            currency,
            executedAt:
                executedAt.toISOString(),
            notes,
        };
    }

    private applyResponse(
        response:
            PagedResponse<TransactionResponse>,
    ): void {
        this.transactions =
            response.content;
        this.currentPage =
            response.page;
        this.pageSize =
            response.size;
        this.totalElements =
            response.totalElements;
        this.totalPages =
            response.totalPages;
        this.isFirstPage =
            response.first;
        this.isLastPage =
            response.last;
    }

    private createEmptyFormValue():
        TransactionFormValue {
        return {
            assetId: '',
            transactionType: 'BUY',
            quantity: null,
            unitPrice: null,
            fee: 0,
            totalAmount: null,
            currency: 'USD',
            executedAt:
                this.createCurrentDateTimeValue(),
            notes: '',
        };
    }

    private createCurrentDateTimeValue():
        string {
        const now = new Date();

        const timezoneOffset =
            now.getTimezoneOffset() *
            60_000;

        return new Date(
            now.getTime() -
            timezoneOffset,
        )
            .toISOString()
            .slice(0, 16);
    }

    private toDateTimeLocalValue(
        value: string,
    ): string {
        const date = new Date(value);

        if (
            Number.isNaN(
                date.getTime(),
            )
        ) {
            return '';
        }

        const timezoneOffset =
            date.getTimezoneOffset() *
            60_000;

        return new Date(
            date.getTime() -
            timezoneOffset,
        )
            .toISOString()
            .slice(0, 16);
    }

    private startOfLocalDayToIso(
        value: string,
    ): string {
        return new Date(
            `${value}T00:00:00`,
        ).toISOString();
    }

    private endOfLocalDayToIso(
        value: string,
    ): string {
        return new Date(
            `${value}T23:59:59.999`,
        ).toISOString();
    }

    private normalizeOptionalText(
        value: string,
    ): string | null {
        const normalized =
            value.trim();

        return normalized || null;
    }

    private resetFilters(): void {
        this.filterAssetId = '';
        this.filterTransactionType = '';
        this.filterStartDate = '';
        this.filterEndDate = '';
    }

    private resetPortfolioData(): void {
        this.selectedPortfolioId = '';
        this.assets = [];
        this.transactions = [];
        this.currentPage = 0;
        this.totalElements = 0;
        this.totalPages = 0;
        this.isFirstPage = true;
        this.isLastPage = true;
    }

    private closeCompletedDialog(): void {
        this.dialogMode = null;
        this.selectedTransaction = null;
        this.actionErrorMessage = '';
    }

    private startSubmission(): void {
        this.isSubmitting = true;
        this.actionErrorMessage = '';
        this.changeDetectorRef.markForCheck();
    }

    private finishSubmission(): void {
        this.isSubmitting = false;
        this.changeDetectorRef.markForCheck();
    }

    private resolvePortfolioLoadError(
        error: unknown,
    ): string {
        if (
            !(error instanceof HttpErrorResponse)
        ) {
            return (
                'Portfolios could not be loaded. ' +
                'Please try again.'
            );
        }

        if (error.status === 0) {
            return (
                'The server could not be reached. ' +
                'Check that the backend is running.'
            );
        }

        if (error.status === 401) {
            return (
                'Your session has expired. ' +
                'Please log in again.'
            );
        }

        if (error.status === 403) {
            return (
                'You do not have permission ' +
                'to view portfolios.'
            );
        }

        return (
            'Portfolios could not be loaded. ' +
            'Please try again.'
        );
    }

    private resolveTransactionLoadError(
        error: unknown,
    ): string {
        if (
            !(error instanceof HttpErrorResponse)
        ) {
            return (
                'Transactions could not be loaded. ' +
                'Please try again.'
            );
        }

        if (error.status === 0) {
            return (
                'The server could not be reached. ' +
                'Check that the backend is running.'
            );
        }

        if (error.status === 401) {
            return (
                'Your session has expired. ' +
                'Please log in again.'
            );
        }

        if (error.status === 403) {
            return (
                'You do not have permission ' +
                'to view transactions.'
            );
        }

        if (error.status === 404) {
            return (
                'The selected portfolio ' +
                'could not be found.'
            );
        }

        if (error.status === 400) {
            return (
                this.extractBackendMessage(error) ??
                'The transaction filters are invalid.'
            );
        }

        return (
            'Transactions could not be loaded. ' +
            'Please try again.'
        );
    }

    private resolveActionError(
        error: unknown,
        action: TransactionAction,
    ): string {
        if (
            !(error instanceof HttpErrorResponse)
        ) {
            return this.defaultActionError(
                action,
            );
        }

        if (error.status === 0) {
            return (
                'The server could not be reached. ' +
                'Check that the backend is running.'
            );
        }

        if (error.status === 401) {
            return (
                'Your session has expired. ' +
                'Please log in again.'
            );
        }

        if (error.status === 403) {
            return (
                'You do not have permission ' +
                'to perform this action.'
            );
        }

        if (error.status === 404) {
            return (
                'The portfolio, transaction or asset ' +
                'could not be found.'
            );
        }

        if (error.status === 409) {
            return (
                this.extractBackendMessage(error) ??
                'The transaction conflicts with existing data.'
            );
        }

        if (error.status === 400) {
            return (
                this.extractBackendMessage(error) ??
                'The submitted transaction information is invalid.'
            );
        }

        return (
            this.extractBackendMessage(error) ??
            this.defaultActionError(action)
        );
    }

    private extractBackendMessage(
        error: HttpErrorResponse,
    ): string | null {
        const responseBody =
            error.error;

        if (
            responseBody &&
            typeof responseBody === 'object'
        ) {
            const candidate =
                'message' in responseBody
                    ? responseBody.message
                    : 'detail' in responseBody
                        ? responseBody.detail
                        : null;

            if (
                typeof candidate === 'string' &&
                candidate.trim()
            ) {
                return candidate.trim();
            }
        }

        if (
            typeof responseBody === 'string' &&
            responseBody.trim()
        ) {
            return responseBody.trim();
        }

        return null;
    }

    private defaultActionError(
        action: TransactionAction,
    ): string {
        switch (action) {
            case 'create':
                return (
                    'The transaction could not be created.'
                );

            case 'edit':
                return (
                    'The transaction could not be updated.'
                );

            case 'delete':
                return (
                    'The transaction could not be deleted.'
                );
        }
    }

    private clearMessages(): void {
        this.actionErrorMessage = '';
        this.successMessage = '';
    }
}