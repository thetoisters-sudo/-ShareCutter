import {
    CurrencyPipe,
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
    DestroyRef,
    OnInit,
    inject,
} from '@angular/core';
import {
    takeUntilDestroyed,
} from '@angular/core/rxjs-interop';
import {
    FormsModule,
} from '@angular/forms';
import {
    ActivatedRoute,
    Router,
} from '@angular/router';
import {
    Observable,
    forkJoin,
    map,
    of,
    switchMap,
} from 'rxjs';

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
    PagedResponse,
    PortfolioResponse,
    PortfolioSummaryResponse,
} from '../../../../core/portfolio/models/portfolio.models';
import {
    PortfolioService,
} from '../../../../core/portfolio/services/portfolio.service';
import {
    TransactionResponse,
    TransactionType,
} from '../../../../core/transaction/models/transaction.models';
import {
    TransactionService,
} from '../../../../core/transaction/services/transaction.service';

type HistoryTypeFilter =
    | 'ALL'
    | TransactionType;

interface HistoryTransaction {
    transaction: TransactionResponse;
    assetSymbol: string;
    assetName: string;
}

@Component({
    selector: 'app-portfolio-history-page',
    imports: [
        CurrencyPipe,
        DatePipe,
        DecimalPipe,
        FormsModule,
    ],
    templateUrl:
        './portfolio-history-page.html',
    styleUrl:
        './portfolio-history-page.scss',
    changeDetection:
        ChangeDetectionStrategy.OnPush,
})
export class PortfolioHistoryPage
    implements OnInit {

    private static readonly API_PAGE_SIZE =
        100;

    private static readonly VIEW_PAGE_SIZE =
        20;

    private readonly route =
        inject(ActivatedRoute);

    private readonly router =
        inject(Router);

    private readonly portfolioService =
        inject(PortfolioService);

    private readonly transactionService =
        inject(TransactionService);

    private readonly assetService =
        inject(AssetService);

    private readonly translationService =
        inject(TranslationService);

    private readonly changeDetectorRef =
        inject(ChangeDetectorRef);

    private readonly destroyRef =
        inject(DestroyRef);

    protected readonly text =
        this.translationService.text;

    protected portfolio:
        PortfolioResponse | null = null;

    protected summary:
        PortfolioSummaryResponse | null =
        null;

    protected transactions:
        HistoryTransaction[] = [];

    protected selectedType:
        HistoryTypeFilter = 'ALL';

    protected startDate = '';

    protected endDate = '';

    protected currentPage = 0;

    protected isLoading = true;

    protected errorMessage = '';

    ngOnInit(): void {
        const portfolioId =
            this.route.snapshot.paramMap.get(
                'portfolioId',
            );

        if (!portfolioId) {
            this.errorMessage =
                this.isHebrew
                    ? 'לא נמצא מזהה תיק.'
                    : 'Portfolio id was not found.';

            this.isLoading = false;

            return;
        }

        this.loadHistory(
            portfolioId,
        );
    }

    protected get isHebrew(): boolean {
        return (
            this.text()
                .portfolios
                .cancel ===
            'ביטול'
        );
    }

    protected get pageTitle(): string {
        return this.isHebrew
            ? 'היסטוריית תיק'
            : 'Portfolio history';
    }

    protected get pageDescription(): string {
        return this.isHebrew
            ? (
                'כל הפעולות שבוצעו בתיק, ' +
                'מסודרות מהחדשה לישנה.'
            )
            : (
                'Every action recorded for this portfolio, ' +
                'ordered from newest to oldest.'
            );
    }

    protected get backLabel(): string {
        return this.isHebrew
            ? 'חזרה לתיקים'
            : 'Back to portfolios';
    }

    protected get exportLabel(): string {
        return this.isHebrew
            ? 'ייצוא CSV'
            : 'Export CSV';
    }

    protected get allTypesLabel(): string {
        return this.isHebrew
            ? 'כל הפעולות'
            : 'All actions';
    }

    protected get totalActionsLabel(): string {
        return this.isHebrew
            ? 'סה״כ פעולות'
            : 'Total actions';
    }

    protected get buysLabel(): string {
        return this.isHebrew
            ? 'קניות'
            : 'Buys';
    }

    protected get sellsLabel(): string {
        return this.isHebrew
            ? 'מכירות'
            : 'Sells';
    }

    protected get cashActionsLabel(): string {
        return this.isHebrew
            ? 'פעולות מזומן'
            : 'Cash actions';
    }

    protected get noResultsLabel(): string {
        return this.isHebrew
            ? 'אין פעולות התואמות למסננים.'
            : 'No transactions match the selected filters.';
    }

    protected get filteredTransactions():
        HistoryTransaction[] {
        let result =
            this.transactions;

        if (
            this.selectedType !==
            'ALL'
        ) {
            result =
                result.filter(
                    (item) =>
                        item.transaction
                            .transactionType ===
                        this.selectedType,
                );
        }

        if (this.startDate) {
            const start =
                new Date(
                    `${this.startDate}T00:00:00`,
                );

            result =
                result.filter(
                    (item) =>
                        new Date(
                            item.transaction
                                .executedAt,
                        ) >= start,
                );
        }

        if (this.endDate) {
            const end =
                new Date(
                    `${this.endDate}T23:59:59.999`,
                );

            result =
                result.filter(
                    (item) =>
                        new Date(
                            item.transaction
                                .executedAt,
                        ) <= end,
                );
        }

        return result;
    }

    protected get displayedTransactions():
        HistoryTransaction[] {
        const start =
            this.currentPage *
            PortfolioHistoryPage
                .VIEW_PAGE_SIZE;

        return this.filteredTransactions
            .slice(
                start,
                start +
                PortfolioHistoryPage
                    .VIEW_PAGE_SIZE,
            );
    }

    protected get totalPages(): number {
        return Math.max(
            1,
            Math.ceil(
                this.filteredTransactions
                    .length /
                PortfolioHistoryPage
                    .VIEW_PAGE_SIZE,
            ),
        );
    }

    protected get buyCount(): number {
        return this.transactions.filter(
            (item) =>
                item.transaction
                    .transactionType ===
                'BUY',
        ).length;
    }

    protected get sellCount(): number {
        return this.transactions.filter(
            (item) =>
                item.transaction
                    .transactionType ===
                'SELL',
        ).length;
    }

    protected get cashActionCount():
        number {
        return this.transactions.filter(
            (item) =>
                [
                    'DEPOSIT',
                    'WITHDRAWAL',
                    'TRANSFER_IN',
                    'TRANSFER_OUT',
                ].includes(
                    item.transaction
                        .transactionType,
                ),
        ).length;
    }

    protected applyFilters(): void {
        this.currentPage = 0;
    }

    protected clearFilters(): void {
        this.selectedType = 'ALL';
        this.startDate = '';
        this.endDate = '';
        this.currentPage = 0;
    }

    protected previousPage(): void {
        if (this.currentPage > 0) {
            this.currentPage -= 1;
        }
    }

    protected nextPage(): void {
        if (
            this.currentPage + 1 <
            this.totalPages
        ) {
            this.currentPage += 1;
        }
    }

    protected goBack(): void {
        void this.router.navigate(
            ['/portfolios'],
        );
    }

    protected exportCsv(): void {
        const rows =
            this.filteredTransactions;

        if (
            !this.portfolio ||
            rows.length === 0
        ) {
            return;
        }

        const header = [
            'Executed At',
            'Type',
            'Asset Symbol',
            'Asset Name',
            'Quantity',
            'Unit Price',
            'Fee',
            'Total Amount',
            'Currency',
            'Notes',
        ];

        const dataRows =
            rows.map(
                (item) => {
                    const transaction =
                        item.transaction;

                    return [
                        transaction.executedAt,
                        transaction.transactionType,
                        item.assetSymbol,
                        item.assetName,
                        transaction.quantity ?? '',
                        transaction.unitPrice ?? '',
                        transaction.fee ?? '',
                        transaction.totalAmount,
                        transaction.currency,
                        transaction.notes ?? '',
                    ];
                },
            );

        const csv =
            [
                header,
                ...dataRows,
            ]
                .map(
                    (row) =>
                        row
                            .map(
                                (value) =>
                                    this.escapeCsv(
                                        String(
                                            value,
                                        ),
                                    ),
                            )
                            .join(','),
                )
                .join('\r\n');

        const blob =
            new Blob(
                [
                    '\uFEFF',
                    csv,
                ],
                {
                    type:
                        'text/csv;charset=utf-8;',
                },
            );

        const url =
            URL.createObjectURL(
                blob,
            );

        const anchor =
            document.createElement(
                'a',
            );

        anchor.href = url;

        anchor.download =
            (
                `${this.safeFileName(
                    this.portfolio.name,
                )}-history.csv`
            );

        document.body.appendChild(
            anchor,
        );

        anchor.click();

        anchor.remove();

        URL.revokeObjectURL(
            url,
        );
    }

    protected transactionTypeLabel(
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

            case 'FEE':
                return translations.typeFee;

            case 'DEPOSIT':
                return translations.typeDeposit;

            case 'WITHDRAWAL':
                return translations.typeWithdrawal;

            case 'TRANSFER_IN':
                return translations.typeTransferIn;

            case 'TRANSFER_OUT':
                return translations.typeTransferOut;
        }
    }

    protected isCashOnly(
        transaction:
            TransactionResponse,
    ): boolean {
        return (
            transaction.assetId ===
            null
        );
    }

    private loadHistory(
        portfolioId: string,
    ): void {
        this.isLoading = true;
        this.errorMessage = '';

        forkJoin({
            portfolio:
                this.portfolioService
                    .getPortfolio(
                        portfolioId,
                    ),

            summary:
                this.portfolioService
                    .getPortfolioSummary(
                        portfolioId,
                    ),

            assets:
                this.assetService
                    .getAssets(
                        portfolioId,
                    ),

            transactions:
                this.loadAllTransactions(
                    portfolioId,
                ),
        })
            .pipe(
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: ({
                    portfolio,
                    summary,
                    assets,
                    transactions,
                }) => {
                    this.portfolio =
                        portfolio;

                    this.summary =
                        summary;

                    const assetMap =
                        new Map<
                            string,
                            AssetResponse
                        >(
                            assets.map(
                                (asset) => [
                                    asset.id,
                                    asset,
                                ],
                            ),
                        );

                    this.transactions =
                        transactions
                            .map(
                                (
                                    transaction,
                                ) => {
                                    const asset =
                                        transaction
                                            .assetId
                                            ? assetMap.get(
                                                transaction
                                                    .assetId,
                                            )
                                            : null;

                                    return {
                                        transaction,

                                        assetSymbol:
                                            asset
                                                ?.symbol ??
                                            (
                                                transaction
                                                    .assetId
                                                    ? (
                                                        this.isHebrew
                                                            ? 'נכס לא פעיל'
                                                            : 'Inactive asset'
                                                    )
                                                    : (
                                                        this.isHebrew
                                                            ? 'מזומן'
                                                            : 'Cash'
                                                    )
                                            ),

                                        assetName:
                                            asset
                                                ?.displayName ??
                                            '',
                                    };
                                },
                            )
                            .sort(
                                (
                                    left,
                                    right,
                                ) =>
                                    new Date(
                                        right
                                            .transaction
                                            .executedAt,
                                    ).getTime() -
                                    new Date(
                                        left
                                            .transaction
                                            .executedAt,
                                    ).getTime(),
                            );

                    this.isLoading =
                        false;

                    this.changeDetectorRef
                        .markForCheck();
                },

                error: (
                    error: unknown,
                ) => {
                    this.isLoading =
                        false;

                    this.errorMessage =
                        this.resolveError(
                            error,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    private loadAllTransactions(
        portfolioId: string,
    ): Observable<
        TransactionResponse[]
    > {
        return this.transactionService
            .getTransactions(
                portfolioId,
                {
                    page: 0,
                    size:
                        PortfolioHistoryPage
                            .API_PAGE_SIZE,
                },
            )
            .pipe(
                switchMap(
                    (
                        firstPage:
                            PagedResponse<
                                TransactionResponse
                            >,
                    ) => {
                        if (
                            firstPage.totalPages <=
                            1
                        ) {
                            return of(
                                firstPage.content,
                            );
                        }

                        const requests:
                            Observable<
                                PagedResponse<
                                    TransactionResponse
                                >
                            >[] =
                            [];

                        for (
                            let page = 1;
                            page <
                            firstPage.totalPages;
                            page += 1
                        ) {
                            requests.push(
                                this.transactionService
                                    .getTransactions(
                                        portfolioId,
                                        {
                                            page,
                                            size:
                                                PortfolioHistoryPage
                                                    .API_PAGE_SIZE,
                                        },
                                    ),
                            );
                        }

                        return forkJoin(
                            requests,
                        )
                            .pipe(
                                map(
                                    (
                                        pages,
                                    ) => [
                                            ...firstPage
                                                .content,

                                            ...pages
                                                .flatMap(
                                                    (
                                                        page,
                                                    ) =>
                                                        page
                                                            .content,
                                                ),
                                        ],
                                ),
                            );
                    },
                ),
            );
    }

    private resolveError(
        error: unknown,
    ): string {
        if (
            !(error instanceof
                HttpErrorResponse)
        ) {
            return this.isHebrew
                ? 'לא ניתן לטעון את היסטוריית התיק.'
                : 'Unable to load portfolio history.';
        }

        if (error.status === 0) {
            return this.text()
                .transactions
                .serverUnavailable;
        }

        if (error.status === 401) {
            return this.text()
                .transactions
                .sessionExpired;
        }

        if (error.status === 403) {
            return this.text()
                .transactions
                .forbiddenViewTransactions;
        }

        if (error.status === 404) {
            return this.isHebrew
                ? 'התיק המבוקש לא נמצא.'
                : 'The requested portfolio was not found.';
        }

        return (
            this.extractBackendMessage(
                error,
            ) ??
            this.text()
                .transactions
                .transactionsLoadError
        );
    }

    private extractBackendMessage(
        error: HttpErrorResponse,
    ): string | null {
        const responseBody =
            error.error;

        if (
            responseBody &&
            typeof responseBody ===
            'object' &&
            'message' in responseBody
        ) {
            const message =
                responseBody.message;

            if (
                typeof message ===
                'string' &&
                message.trim()
            ) {
                return message.trim();
            }
        }

        return null;
    }

    private escapeCsv(
        value: string,
    ): string {
        return (
            '"' +
            value.replace(
                /"/g,
                '""',
            ) +
            '"'
        );
    }

    private safeFileName(
        value: string,
    ): string {
        return value
            .trim()
            .replace(
                /[\\/:*?"<>|]+/g,
                '-',
            )
            .replace(
                /\s+/g,
                '-',
            );
    }
}