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
    TranslationService,
} from '../../../../core/i18n/services/translation.service';
import {
    MarketPriceResponse,
} from '../../../../core/market-data/models/market-data.models';
import {
    MarketDataService,
} from '../../../../core/market-data/services/market-data.service';
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

type TransactionCalculationMode =
    | 'BY_QUANTITY'
    | 'BY_AMOUNT';

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

    private readonly translationService =
        inject(TranslationService);

    private readonly marketDataService =
        inject(MarketDataService);

    private readonly changeDetectorRef =
        inject(ChangeDetectorRef);

    protected readonly transactionTypeOptions =
        TRANSACTION_TYPE_OPTIONS;

    protected readonly text =
        this.translationService.text;

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
    protected isLoadingMarketPrice = false;

    protected marketPriceRetrievedAt = '';
    protected marketPriceErrorMessage = '';
    protected isManualPrice = false;

    protected errorMessage = '';
    protected actionErrorMessage = '';
    protected successMessage = '';

    protected dialogMode:
        TransactionDialogMode = null;

    protected calculationMode:
        TransactionCalculationMode =
        'BY_QUANTITY';

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
                this.text().transactions.startDateAfterEndDate;
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

        this.resetMarketPriceState();
        this.isManualPrice = false;
        this.calculationMode =
            'BY_QUANTITY';
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

        this.resetMarketPriceState();
        this.isManualPrice = true;
        this.calculationMode =
            'BY_QUANTITY';
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
        this.resetMarketPriceState();
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
                        this.text().transactions.createdSuccess;
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
                        this.text().transactions.updatedSuccess;
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
                        this.text().transactions.deletedSuccess;

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
        this.actionErrorMessage = '';
        this.resetMarketPriceState();

        if (
            !this.transactionRequiresAsset(
                this.formValue.transactionType,
            )
        ) {
            this.formValue.assetId = '';
        }

        if (
            !this.transactionUsesQuantity(
                this.formValue.transactionType,
            )
        ) {
            this.formValue.quantity = null;
            this.formValue.unitPrice = null;
            this.formValue.totalAmount = null;
            this.isManualPrice = false;
            this.calculationMode =
                'BY_QUANTITY';
            this.changeDetectorRef.markForCheck();
            return;
        }

        this.formValue.quantity = null;
        this.formValue.unitPrice = null;
        this.formValue.totalAmount = null;
        this.isManualPrice =
            this.dialogMode === 'edit';

        if (
            this.formValue.assetId &&
            !this.isManualPrice
        ) {
            this.refreshMarketPrice();
        }

        this.changeDetectorRef.markForCheck();
    }

    protected onTransactionAssetChange(
        assetId: string,
    ): void {
        this.formValue.assetId = assetId;
        this.formValue.quantity = null;
        this.formValue.unitPrice = null;

        if (
            this.calculationMode ===
            'BY_QUANTITY'
        ) {
            this.formValue.totalAmount =
                null;
        }

        this.resetMarketPriceState();
        this.actionErrorMessage = '';

        const asset =
            this.getSelectedFormAsset();

        if (asset) {
            this.formValue.currency =
                asset.currency;
        }

        if (
            asset &&
            this.transactionUsesQuantity(
                this.formValue.transactionType,
            ) &&
            !this.isManualPrice
        ) {
            this.loadLatestMarketPrice(asset);
        }

        this.changeDetectorRef.markForCheck();
    }

    protected setCalculationMode(
        mode:
            TransactionCalculationMode,
    ): void {
        if (
            this.calculationMode ===
            mode
        ) {
            return;
        }

        this.calculationMode = mode;
        this.actionErrorMessage = '';

        if (
            mode === 'BY_AMOUNT'
        ) {
            this.formValue.quantity =
                null;
            this.recalculateQuantityFromTotal();
        } else {
            this.formValue.totalAmount =
                null;
            this.recalculateTotalAmount();
        }

        this.changeDetectorRef
            .markForCheck();
    }

    protected onTransactionQuantityChange(): void {
        if (
            this.calculationMode ===
            'BY_QUANTITY'
        ) {
            this.recalculateTotalAmount();
        }
    }

    protected onTransactionTotalAmountChange():
        void {
        if (
            this.calculationMode ===
            'BY_AMOUNT'
        ) {
            this.recalculateQuantityFromTotal();
        }
    }

    protected onTransactionFeeChange(): void {
        if (
            this.calculationMode ===
            'BY_QUANTITY'
        ) {
            this.recalculateTotalAmount();
        }
    }

    protected onManualUnitPriceChange(): void {
        this.isManualPrice = true;
        this.marketPriceRetrievedAt = '';
        this.marketPriceErrorMessage = '';

        this.recalculateByCurrentMode();
    }

    protected setManualPriceMode(
        enabled: boolean,
    ): void {
        this.isManualPrice = enabled;
        this.marketPriceErrorMessage = '';

        if (!enabled) {
            this.refreshMarketPrice();
            return;
        }

        this.isLoadingMarketPrice = false;
        this.marketPriceRetrievedAt = '';
        this.changeDetectorRef.markForCheck();
    }

    protected refreshMarketPrice(): void {
        if (
            !this.transactionUsesQuantity(
                this.formValue.transactionType,
            )
        ) {
            return;
        }

        const asset =
            this.getSelectedFormAsset();

        if (!asset) {
            this.marketPriceErrorMessage =
                this.text().transactions.selectAssetBeforeMarketPrice;
            this.changeDetectorRef.markForCheck();
            return;
        }

        this.isManualPrice = false;
        this.loadLatestMarketPrice(asset);
    }

    protected get selectedFormAsset():
        AssetResponse | null {
        return this.getSelectedFormAsset();
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
        const translations =
            this.text().transactions;

        switch (type) {
            case 'BUY':
                return translations.typeBuy;

            case 'SELL':
                return translations.typeSell;

            case 'DIVIDEND':
                return translations.typeDividend;

            case 'DEPOSIT':
                return translations.typeDeposit;

            case 'WITHDRAWAL':
                return translations.typeWithdrawal;

            case 'FEE':
                return translations.typeFee;
        }
    }

    protected getAssetLabel(
        assetId: string | null,
    ): string {
        if (!assetId) {
            return this.text().transactions.noAsset;
        }

        const asset =
            this.assets.find(
                (candidate) =>
                    candidate.id === assetId,
            );

        if (!asset) {
            return this.text().transactions.unknownAsset;
        }

        return (
            `${asset.symbol} · ` +
            asset.displayName
        );
    }

    private loadLatestMarketPrice(
        asset: AssetResponse,
    ): void {
        this.isLoadingMarketPrice = true;
        this.marketPriceErrorMessage = '';
        this.marketPriceRetrievedAt = '';
        this.changeDetectorRef.markForCheck();

        this.marketDataService
            .getLatestPrice({
                symbol: asset.symbol,
                exchange: asset.exchange,
            })
            .subscribe({
                next: (
                    response:
                        MarketPriceResponse,
                ) => {
                    if (
                        this.formValue.assetId !==
                        asset.id
                    ) {
                        return;
                    }

                    this.formValue.unitPrice =
                        response.price;
                    this.formValue.currency =
                        asset.currency;
                    this.marketPriceRetrievedAt =
                        response.retrievedAt;
                    this.isLoadingMarketPrice = false;
                    this.marketPriceErrorMessage = '';
                    this.recalculateByCurrentMode();
                    this.changeDetectorRef.markForCheck();
                },
                error: (error: unknown) => {
                    if (
                        this.formValue.assetId !==
                        asset.id
                    ) {
                        return;
                    }

                    this.isLoadingMarketPrice = false;
                    this.marketPriceRetrievedAt = '';
                    this.marketPriceErrorMessage =
                        this.resolveMarketPriceError(
                            error,
                        );
                    this.isManualPrice = true;
                    this.changeDetectorRef.markForCheck();
                },
            });
    }

    private recalculateByCurrentMode(): void {
        if (
            this.calculationMode ===
            'BY_AMOUNT'
        ) {
            this.recalculateQuantityFromTotal();
            return;
        }

        this.recalculateTotalAmount();
    }

    private recalculateQuantityFromTotal(): void {
        if (
            !this.transactionUsesQuantity(
                this.formValue.transactionType,
            )
        ) {
            return;
        }

        const totalAmount =
            this.formValue.totalAmount;

        const unitPrice =
            this.formValue.unitPrice;

        if (
            totalAmount === null ||
            totalAmount <= 0 ||
            unitPrice === null ||
            unitPrice <= 0
        ) {
            this.formValue.quantity =
                null;

            this.changeDetectorRef
                .markForCheck();

            return;
        }

        this.formValue.quantity =
            this.roundToEightDecimals(
                totalAmount /
                unitPrice,
            );

        this.changeDetectorRef
            .markForCheck();
    }

    private recalculateTotalAmount(): void {
        if (
            !this.transactionUsesQuantity(
                this.formValue.transactionType,
            )
        ) {
            return;
        }

        const quantity =
            this.formValue.quantity;

        const unitPrice =
            this.formValue.unitPrice;

        if (
            quantity === null ||
            quantity <= 0 ||
            unitPrice === null ||
            unitPrice < 0
        ) {
            this.formValue.totalAmount = null;
            this.changeDetectorRef.markForCheck();
            return;
        }

        const fee =
            this.formValue.fee ?? 0;

        const grossAmount =
            quantity * unitPrice;

        const totalAmount =
            this.formValue.transactionType ===
                'SELL'
                ? grossAmount - fee
                : grossAmount + fee;

        this.formValue.totalAmount =
            this.roundToEightDecimals(
                Math.max(
                    totalAmount,
                    0,
                ),
            );

        this.changeDetectorRef.markForCheck();
    }

    private roundToEightDecimals(
        value: number,
    ): number {
        return Math.round(
            (
                value +
                Number.EPSILON
            ) *
            100_000_000,
        ) / 100_000_000;
    }

    private getSelectedFormAsset():
        AssetResponse | null {
        return (
            this.assets.find(
                (asset) =>
                    asset.id ===
                    this.formValue.assetId,
            ) ?? null
        );
    }

    private resetMarketPriceState(): void {
        this.isLoadingMarketPrice = false;
        this.marketPriceRetrievedAt = '';
        this.marketPriceErrorMessage = '';
    }

    private resolveMarketPriceError(
        error: unknown,
    ): string {
        const translations =
            this.text().transactions;

        if (
            !(error instanceof HttpErrorResponse)
        ) {
            return translations.marketPriceError;
        }

        if (error.status === 0) {
            return translations.marketServiceUnavailable;
        }

        if (error.status === 401) {
            return translations.sessionExpired;
        }

        if (error.status === 403) {
            return translations.marketPriceForbidden;
        }

        if (error.status === 404) {
            return translations.marketPriceNotFound;
        }

        return (
            this.extractBackendMessage(error) ??
            translations.marketPriceError
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

        if (usesQuantity) {
            this.recalculateByCurrentMode();
        }

        if (
            requiresAsset &&
            !this.formValue.assetId
        ) {
            this.actionErrorMessage =
                this.text().transactions.selectAssetRequired;
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
                this.text().transactions.quantityInvalid;
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
                this.text().transactions.unitPriceInvalid;
            return null;
        }

        if (
            this.formValue.fee !== null &&
            this.formValue.fee < 0
        ) {
            this.actionErrorMessage =
                this.text().transactions.feeInvalid;
            return null;
        }

        if (
            this.formValue.totalAmount ===
            null ||
            this.formValue.totalAmount < 0
        ) {
            this.actionErrorMessage =
                this.text().transactions.totalAmountInvalid;
            return null;
        }

        if (!/^[A-Z]{3}$/.test(currency)) {
            this.actionErrorMessage =
                this.text().transactions.currencyInvalid;
            return null;
        }

        if (!this.formValue.executedAt) {
            this.actionErrorMessage =
                this.text().transactions.executionTimeRequired;
            return null;
        }

        if (
            notes !== null &&
            notes.length > 2000
        ) {
            this.actionErrorMessage =
                this.text().transactions.notesTooLong;
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
                this.text().transactions.executionTimeInvalid;
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
        const translations =
            this.text().transactions;

        if (
            !(error instanceof HttpErrorResponse)
        ) {
            return translations.portfolioLoadError;
        }

        if (error.status === 0) {
            return translations.serverUnavailable;
        }

        if (error.status === 401) {
            return translations.sessionExpired;
        }

        if (error.status === 403) {
            return translations.forbiddenViewPortfolios;
        }

        return translations.portfolioLoadError;
    }

    private resolveTransactionLoadError(
        error: unknown,
    ): string {
        const translations =
            this.text().transactions;

        if (
            !(error instanceof HttpErrorResponse)
        ) {
            return translations.transactionsLoadError;
        }

        if (error.status === 0) {
            return translations.serverUnavailable;
        }

        if (error.status === 401) {
            return translations.sessionExpired;
        }

        if (error.status === 403) {
            return translations.forbiddenViewTransactions;
        }

        if (error.status === 404) {
            return translations.selectedPortfolioNotFound;
        }

        if (error.status === 400) {
            return (
                this.extractBackendMessage(error) ??
                translations.invalidFilters
            );
        }

        return translations.transactionsLoadError;
    }

    private resolveActionError(
        error: unknown,
        action: TransactionAction,
    ): string {
        const translations =
            this.text().transactions;

        if (
            !(error instanceof HttpErrorResponse)
        ) {
            return this.defaultActionError(
                action,
            );
        }

        if (error.status === 0) {
            return translations.serverUnavailable;
        }

        if (error.status === 401) {
            return translations.sessionExpired;
        }

        if (error.status === 403) {
            return translations.forbiddenAction;
        }

        if (error.status === 404) {
            return translations.transactionResourceNotFound;
        }

        if (error.status === 409) {
            return (
                this.extractBackendMessage(error) ??
                translations.transactionConflict
            );
        }

        if (error.status === 400) {
            return (
                this.extractBackendMessage(error) ??
                translations.invalidTransaction
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
        const translations =
            this.text().transactions;

        switch (action) {
            case 'create':
                return translations.createError;

            case 'edit':
                return translations.editError;

            case 'delete':
                return translations.deleteError;
        }
    }

    private clearMessages(): void {
        this.actionErrorMessage = '';
        this.successMessage = '';
    }
}