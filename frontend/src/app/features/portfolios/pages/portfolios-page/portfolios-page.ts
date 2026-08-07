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
    Observable,
    catchError,
    forkJoin,
    map,
    of,
    switchMap,
} from 'rxjs';

import {
    TranslationService,
} from '../../../../core/i18n/services/translation.service';
import {
    PagedResponse,
    PortfolioAllocationResponse,
    PortfolioCreateRequest,
    PortfolioCreationMethod,
    PortfolioResponse,
    PortfolioSummaryResponse,
} from '../../../../core/portfolio/models/portfolio.models';
import {
    PortfolioChangeEvent,
    PortfolioService,
} from '../../../../core/portfolio/services/portfolio.service';

type PortfolioDialogMode =
    | 'create'
    | 'rename'
    | 'delete'
    | null;

type PortfolioAction =
    | 'create'
    | 'rename'
    | 'delete';

interface PortfolioListItem {
    id: string;
    userId: string;
    name: string;
    creationMethod: PortfolioCreationMethod;
    initialValue: number;
    currentValue: number;
    totalRealizedProfit: number;
    totalUnrealizedProfit: number;
    totalProfit: number;
    totalReturnPercent: number;
    activeAssetCount: number;
    transactionCount: number;
    calculatedAt: string;
    createdAt: string;
    updatedAt: string;
    analyticsAvailable: boolean;
}

interface PortfolioPageResult {
    response:
    PagedResponse<PortfolioResponse>;
    portfolios:
    PortfolioListItem[];
}

@Component({
    selector: 'app-portfolios-page',
    imports: [
        CurrencyPipe,
        DatePipe,
        DecimalPipe,
        FormsModule,
    ],
    templateUrl:
        './portfolios-page.html',
    styleUrl:
        './portfolios-page.scss',
    changeDetection:
        ChangeDetectionStrategy.OnPush,
})
export class PortfoliosPage
    implements OnInit {

    private readonly portfolioService =
        inject(PortfolioService);

    private readonly translationService =
        inject(TranslationService);

    private readonly changeDetectorRef =
        inject(ChangeDetectorRef);

    private readonly destroyRef =
        inject(DestroyRef);

    protected readonly text =
        this.translationService.text;

    protected portfolios:
        PortfolioListItem[] = [];

    protected currentPage = 0;
    protected pageSize = 6;
    protected totalElements = 0;
    protected totalPages = 0;
    protected isFirstPage = true;
    protected isLastPage = true;

    protected isLoading = true;
    protected isSubmitting = false;
    protected errorMessage = '';
    protected actionErrorMessage = '';
    protected successMessage = '';

    protected dialogMode:
        PortfolioDialogMode = null;

    protected selectedPortfolio:
        PortfolioListItem | null = null;

    protected createName = '';

    protected createMethod:
        PortfolioCreationMethod =
        'BY_AMOUNT';

    protected createInitialValue:
        number | null = null;

    protected renameValue = '';

    protected expandedPortfolioId:
        string | null = null;

    private readonly allocations =
        new Map<
            string,
            PortfolioAllocationResponse
        >();

    private readonly allocationLoadingIds =
        new Set<string>();

    private readonly allocationErrors =
        new Map<string, string>();

    ngOnInit(): void {
        this.subscribeToPortfolioChanges();
        this.loadPortfolios();
    }

    protected retry(): void {
        this.loadPortfolios();
    }

    protected goToPreviousPage(): void {
        if (
            this.isFirstPage ||
            this.isLoading
        ) {
            return;
        }

        this.currentPage -= 1;
        this.loadPortfolios();
    }

    protected goToNextPage(): void {
        if (
            this.isLastPage ||
            this.isLoading
        ) {
            return;
        }

        this.currentPage += 1;
        this.loadPortfolios();
    }

    protected togglePortfolioDetails(
        portfolio: PortfolioListItem,
    ): void {
        if (
            this.expandedPortfolioId ===
            portfolio.id
        ) {
            this.expandedPortfolioId =
                null;

            this.changeDetectorRef
                .markForCheck();

            return;
        }

        this.expandedPortfolioId =
            portfolio.id;

        if (
            !this.allocations.has(
                portfolio.id,
            ) &&
            !this.allocationLoadingIds.has(
                portfolio.id,
            )
        ) {
            this.loadPortfolioAllocation(
                portfolio.id,
            );
        }

        this.changeDetectorRef
            .markForCheck();
    }

    protected retryPortfolioAllocation(
        portfolioId: string,
    ): void {
        this.allocationErrors.delete(
            portfolioId,
        );

        this.allocations.delete(
            portfolioId,
        );

        this.loadPortfolioAllocation(
            portfolioId,
        );
    }

    protected isPortfolioExpanded(
        portfolioId: string,
    ): boolean {
        return (
            this.expandedPortfolioId ===
            portfolioId
        );
    }

    protected isAllocationLoading(
        portfolioId: string,
    ): boolean {
        return this.allocationLoadingIds
            .has(portfolioId);
    }

    protected getPortfolioAllocation(
        portfolioId: string,
    ): PortfolioAllocationResponse | null {
        return (
            this.allocations.get(
                portfolioId,
            ) ??
            null
        );
    }

    protected getAllocationError(
        portfolioId: string,
    ): string {
        return (
            this.allocationErrors.get(
                portfolioId,
            ) ??
            ''
        );
    }

    protected openCreateDialog(): void {
        this.clearMessages();
        this.selectedPortfolio = null;
        this.createName = '';
        this.createMethod = 'BY_AMOUNT';
        this.createInitialValue = null;
        this.dialogMode = 'create';
    }

    protected openRenameDialog(
        portfolio: PortfolioListItem,
    ): void {
        this.clearMessages();
        this.selectedPortfolio =
            portfolio;
        this.renameValue =
            portfolio.name;
        this.dialogMode =
            'rename';
    }

    protected openDeleteDialog(
        portfolio: PortfolioListItem,
    ): void {
        this.clearMessages();
        this.selectedPortfolio =
            portfolio;
        this.dialogMode =
            'delete';
    }

    protected closeDialog(): void {
        if (this.isSubmitting) {
            return;
        }

        this.dialogMode = null;
        this.selectedPortfolio = null;
        this.actionErrorMessage = '';
    }

    protected submitCreate(): void {
        const normalizedName =
            this.createName.trim();

        if (!normalizedName) {
            this.actionErrorMessage =
                this.text()
                    .portfolios
                    .portfolioNameRequired;

            return;
        }

        if (
            this.createInitialValue ===
            null ||
            this.createInitialValue < 0
        ) {
            this.actionErrorMessage =
                this.text()
                    .portfolios
                    .initialValueInvalid;

            return;
        }

        const request:
            PortfolioCreateRequest = {
            name:
                normalizedName,
            creationMethod:
                this.createMethod,
            initialValue:
                this.createInitialValue,
        };

        this.startSubmission();

        this.portfolioService
            .createPortfolio(request)
            .subscribe({
                next: (
                    createdPortfolio,
                ) => {
                    this.portfolioService
                        .notifyPortfolioChanged({
                            portfolioId:
                                createdPortfolio.id,
                            reason:
                                'created',
                        });

                    this.finishSuccessfulSubmission(
                        this.text()
                            .portfolios
                            .createdSuccess,
                    );

                    this.currentPage = 0;

                    this.loadPortfolios(
                        false,
                    );
                },

                error: (
                    error: unknown,
                ) => {
                    this.finishFailedSubmission(
                        this.resolveActionError(
                            error,
                            'create',
                        ),
                    );
                },
            });
    }

    protected submitRename(): void {
        const portfolio =
            this.selectedPortfolio;

        const normalizedName =
            this.renameValue.trim();

        if (!portfolio) {
            return;
        }

        if (!normalizedName) {
            this.actionErrorMessage =
                this.text()
                    .portfolios
                    .portfolioNameRequired;

            return;
        }

        this.startSubmission();

        this.portfolioService
            .renamePortfolio(
                portfolio.id,
                {
                    name:
                        normalizedName,
                },
            )
            .subscribe({
                next: () => {
                    this.portfolioService
                        .notifyPortfolioChanged({
                            portfolioId:
                                portfolio.id,
                            reason:
                                'renamed',
                        });

                    this.finishSuccessfulSubmission(
                        this.text()
                            .portfolios
                            .renamedSuccess,
                    );

                    this.loadPortfolios(
                        false,
                    );
                },

                error: (
                    error: unknown,
                ) => {
                    this.finishFailedSubmission(
                        this.resolveActionError(
                            error,
                            'rename',
                        ),
                    );
                },
            });
    }

    protected submitDelete(): void {
        const portfolio =
            this.selectedPortfolio;

        if (!portfolio) {
            return;
        }

        this.startSubmission();

        this.portfolioService
            .deletePortfolio(
                portfolio.id,
            )
            .subscribe({
                next: () => {
                    const
                        shouldMoveToPreviousPage =
                            (
                                this.portfolios
                                    .length === 1 &&
                                this.currentPage > 0
                            );

                    this.portfolioService
                        .notifyPortfolioChanged({
                            portfolioId:
                                portfolio.id,
                            reason:
                                'deleted',
                        });

                    this.finishSuccessfulSubmission(
                        this.text()
                            .portfolios
                            .deletedSuccess,
                    );

                    if (
                        shouldMoveToPreviousPage
                    ) {
                        this.currentPage -= 1;
                    }

                    this.loadPortfolios(
                        false,
                    );
                },

                error: (
                    error: unknown,
                ) => {
                    this.finishFailedSubmission(
                        this.resolveActionError(
                            error,
                            'delete',
                        ),
                    );
                },
            });
    }

    protected dismissSuccessMessage():
        void {
        this.successMessage = '';
    }

    protected isPositiveOrZero(
        value: number,
    ): boolean {
        return value >= 0;
    }

    protected get visiblePageNumber():
        number {
        if (
            this.totalPages === 0
        ) {
            return 0;
        }

        return (
            this.currentPage + 1
        );
    }

    protected get analyticsUnavailableCount():
        number {
        return this.portfolios.filter(
            (portfolio) =>
                !portfolio
                    .analyticsAvailable,
        ).length;
    }

    protected get totalCurrentValue():
        number {
        return this.portfolios.reduce(
            (
                total,
                portfolio,
            ) =>
                total +
                portfolio.currentValue,
            0,
        );
    }

    protected get displayedProfit():
        number {
        return this.portfolios.reduce(
            (
                total,
                portfolio,
            ) =>
                total +
                portfolio.totalProfit,
            0,
        );
    }

    private subscribeToPortfolioChanges():
        void {
        this.portfolioService
            .portfolioChanged$
            .pipe(
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe(
                (
                    event:
                        PortfolioChangeEvent,
                ) => {
                    this.refreshAfterPortfolioChange(
                        event,
                    );
                },
            );
    }

    private refreshAfterPortfolioChange(
        event: PortfolioChangeEvent,
    ): void {
        if (
            event.reason === 'created' ||
            event.reason === 'renamed' ||
            event.reason === 'deleted'
        ) {
            return;
        }

        const expandedPortfolioId =
            this.expandedPortfolioId;

        this.loadPortfolios(false);

        if (
            expandedPortfolioId &&
            (
                event.portfolioId ===
                null ||
                event.portfolioId ===
                expandedPortfolioId
            )
        ) {
            this.allocations.delete(
                expandedPortfolioId,
            );

            this.allocationErrors.delete(
                expandedPortfolioId,
            );
        }
    }

    private loadPortfolios(
        clearMessages = true,
    ): void {
        this.isLoading = true;
        this.errorMessage = '';

        this.resetPortfolioDetails();

        if (clearMessages) {
            this.successMessage = '';
        }

        this.changeDetectorRef
            .markForCheck();

        this.portfolioService
            .getPortfolios({
                page:
                    this.currentPage,
                size:
                    this.pageSize,
                sortBy:
                    'createdAt',
                sortDirection:
                    'desc',
            })
            .pipe(
                switchMap(
                    (response) =>
                        this.loadPortfolioSummaries(
                            response,
                        ),
                ),
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: (result) => {
                    this.applyResponse(
                        result.response,
                        result.portfolios,
                    );

                    this.isLoading =
                        false;

                    this.changeDetectorRef
                        .markForCheck();
                },

                error: (
                    error: unknown,
                ) => {
                    this.applyLoadFailure(
                        this.resolveLoadError(
                            error,
                        ),
                    );
                },
            });
    }

    private loadPortfolioSummaries(
        response:
            PagedResponse<PortfolioResponse>,
    ): Observable<PortfolioPageResult> {
        if (
            response.content.length ===
            0
        ) {
            return of({
                response,
                portfolios: [],
            });
        }

        const summaryRequests =
            response.content.map(
                (portfolio) =>
                    this.loadPortfolioSummary(
                        portfolio,
                    ),
            );

        return forkJoin(
            summaryRequests,
        ).pipe(
            map(
                (portfolios) => ({
                    response,
                    portfolios,
                }),
            ),
        );
    }

    private loadPortfolioSummary(
        portfolio: PortfolioResponse,
    ): Observable<PortfolioListItem> {
        return this.portfolioService
            .getPortfolioSummary(
                portfolio.id,
            )
            .pipe(
                map(
                    (summary) =>
                        this.createPortfolioListItem(
                            portfolio,
                            summary,
                        ),
                ),

                catchError(
                    () =>
                        of(
                            this.createFallbackPortfolio(
                                portfolio,
                            ),
                        ),
                ),
            );
    }

    private loadPortfolioAllocation(
        portfolioId: string,
    ): void {
        this.allocationLoadingIds
            .add(portfolioId);

        this.allocationErrors
            .delete(portfolioId);

        this.changeDetectorRef
            .markForCheck();

        this.portfolioService
            .getPortfolioAllocation(
                portfolioId,
            )
            .pipe(
                takeUntilDestroyed(
                    this.destroyRef,
                ),
            )
            .subscribe({
                next: (
                    allocation,
                ) => {
                    this.allocations.set(
                        portfolioId,
                        allocation,
                    );

                    this.allocationLoadingIds
                        .delete(
                            portfolioId,
                        );

                    this.allocationErrors
                        .delete(
                            portfolioId,
                        );

                    this.changeDetectorRef
                        .markForCheck();
                },

                error: (
                    error: unknown,
                ) => {
                    this.allocations.delete(
                        portfolioId,
                    );

                    this.allocationLoadingIds
                        .delete(
                            portfolioId,
                        );

                    this.allocationErrors.set(
                        portfolioId,
                        this.resolveAllocationError(
                            error,
                        ),
                    );

                    this.changeDetectorRef
                        .markForCheck();
                },
            });
    }

    private createPortfolioListItem(
        portfolio: PortfolioResponse,
        summary:
            PortfolioSummaryResponse,
    ): PortfolioListItem {
        return {
            id:
                portfolio.id,

            userId:
                portfolio.userId,

            name:
                summary.portfolioName,

            creationMethod:
                portfolio.creationMethod,

            initialValue:
                summary.initialValue,

            currentValue:
                summary.currentValue,

            totalRealizedProfit:
                summary
                    .totalRealizedProfit,

            totalUnrealizedProfit:
                summary
                    .totalUnrealizedProfit,

            totalProfit:
                summary.totalProfit,

            totalReturnPercent:
                summary
                    .totalReturnPercent,

            activeAssetCount:
                summary
                    .activeAssetCount,

            transactionCount:
                summary
                    .transactionCount,

            calculatedAt:
                summary.calculatedAt,

            createdAt:
                portfolio.createdAt,

            updatedAt:
                portfolio.updatedAt,

            analyticsAvailable:
                true,
        };
    }

    private createFallbackPortfolio(
        portfolio: PortfolioResponse,
    ): PortfolioListItem {
        return {
            id:
                portfolio.id,

            userId:
                portfolio.userId,

            name:
                portfolio.name,

            creationMethod:
                portfolio.creationMethod,

            initialValue:
                portfolio.initialValue,

            currentValue:
                portfolio.currentValue,

            totalRealizedProfit:
                portfolio
                    .totalRealizedProfit,

            totalUnrealizedProfit:
                portfolio
                    .totalUnrealizedProfit,

            totalProfit:
                (
                    portfolio.currentValue -
                    portfolio.initialValue
                ),

            totalReturnPercent:
                portfolio
                    .totalReturnPercent,

            activeAssetCount:
                0,

            transactionCount:
                0,

            calculatedAt:
                portfolio.updatedAt,

            createdAt:
                portfolio.createdAt,

            updatedAt:
                portfolio.updatedAt,

            analyticsAvailable:
                false,
        };
    }

    private applyResponse(
        response:
            PagedResponse<PortfolioResponse>,
        portfolios:
            PortfolioListItem[],
    ): void {
        this.portfolios =
            portfolios;

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

    private applyLoadFailure(
        message: string,
    ): void {
        this.isLoading = false;
        this.portfolios = [];
        this.totalElements = 0;
        this.totalPages = 0;
        this.isFirstPage = true;
        this.isLastPage = true;
        this.errorMessage = message;

        this.changeDetectorRef
            .markForCheck();
    }

    private resetPortfolioDetails():
        void {
        this.expandedPortfolioId =
            null;

        this.allocations.clear();

        this.allocationLoadingIds
            .clear();

        this.allocationErrors.clear();
    }

    private startSubmission(): void {
        this.isSubmitting = true;
        this.actionErrorMessage = '';

        this.changeDetectorRef
            .markForCheck();
    }

    private finishSuccessfulSubmission(
        message: string,
    ): void {
        this.isSubmitting = false;
        this.dialogMode = null;
        this.selectedPortfolio = null;
        this.successMessage = message;

        this.changeDetectorRef
            .markForCheck();
    }

    private finishFailedSubmission(
        message: string,
    ): void {
        this.isSubmitting = false;
        this.actionErrorMessage =
            message;

        this.changeDetectorRef
            .markForCheck();
    }

    private resolveLoadError(
        error: unknown,
    ): string {
        const translations =
            this.text().portfolios;

        if (
            !(error instanceof
                HttpErrorResponse)
        ) {
            return translations
                .loadError;
        }

        if (
            error.status === 0
        ) {
            return translations
                .serverUnavailable;
        }

        if (
            error.status === 401
        ) {
            return translations
                .sessionExpired;
        }

        if (
            error.status === 403
        ) {
            return translations
                .forbiddenView;
        }

        return translations
            .loadError;
    }

    private resolveAllocationError(
        error: unknown,
    ): string {
        const translations =
            this.text().portfolios;

        if (
            !(error instanceof
                HttpErrorResponse)
        ) {
            return translations
                .allocationLoadError;
        }

        if (
            error.status === 0
        ) {
            return translations
                .serverUnavailable;
        }

        if (
            error.status === 401
        ) {
            return translations
                .sessionExpired;
        }

        if (
            error.status === 403
        ) {
            return translations
                .allocationForbidden;
        }

        if (
            error.status === 404
        ) {
            return translations
                .allocationNotFound;
        }

        return (
            this.extractBackendMessage(
                error,
            ) ??
            translations
                .allocationLoadError
        );
    }

    private resolveActionError(
        error: unknown,
        action:
            PortfolioAction,
    ): string {
        const translations =
            this.text().portfolios;

        if (
            !(error instanceof
                HttpErrorResponse)
        ) {
            return this
                .defaultActionError(
                    action,
                );
        }

        if (
            error.status === 0
        ) {
            return translations
                .serverUnavailable;
        }

        if (
            error.status === 401
        ) {
            return translations
                .sessionExpired;
        }

        if (
            error.status === 403
        ) {
            return translations
                .forbiddenAction;
        }

        if (
            error.status === 409
        ) {
            if (
                action === 'create' ||
                action === 'rename'
            ) {
                return translations
                    .duplicateName;
            }

            return translations
                .portfolioConflict;
        }

        if (
            error.status === 400
        ) {
            return (
                this.extractBackendMessage(
                    error,
                ) ??
                translations
                    .invalidPortfolio
            );
        }

        return (
            this.extractBackendMessage(
                error,
            ) ??
            this.defaultActionError(
                action,
            )
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
            'object'
        ) {
            const candidate =
                'message' in responseBody
                    ? responseBody.message
                    : 'detail' in responseBody
                        ? responseBody.detail
                        : null;

            if (
                typeof candidate ===
                'string' &&
                candidate.trim()
            ) {
                return candidate.trim();
            }
        }

        if (
            typeof responseBody ===
            'string' &&
            responseBody.trim()
        ) {
            return responseBody.trim();
        }

        return null;
    }

    private defaultActionError(
        action:
            PortfolioAction,
    ): string {
        const translations =
            this.text().portfolios;

        switch (action) {
            case 'create':
                return translations
                    .createError;

            case 'rename':
                return translations
                    .renameError;

            case 'delete':
                return translations
                    .deleteError;
        }
    }

    private clearMessages(): void {
        this.actionErrorMessage = '';
        this.successMessage = '';
    }
}