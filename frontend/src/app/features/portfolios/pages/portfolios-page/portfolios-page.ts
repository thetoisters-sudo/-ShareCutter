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
    OnInit,
    inject,
} from '@angular/core';
import {
    FormsModule,
} from '@angular/forms';

import {
    PagedResponse,
    PortfolioCreateRequest,
    PortfolioCreationMethod,
    PortfolioResponse,
} from '../../../../core/portfolio/models/portfolio.models';
import {
    PortfolioService,
} from '../../../../core/portfolio/services/portfolio.service';

type PortfolioDialogMode =
    | 'create'
    | 'rename'
    | 'value'
    | 'delete'
    | null;

type PortfolioAction =
    | 'create'
    | 'rename'
    | 'update'
    | 'delete';

@Component({
    selector: 'app-portfolios-page',
    imports: [
        CurrencyPipe,
        DatePipe,
        DecimalPipe,
        FormsModule,
    ],
    templateUrl: './portfolios-page.html',
    styleUrl: './portfolios-page.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PortfoliosPage implements OnInit {
    private readonly portfolioService =
        inject(PortfolioService);

    private readonly changeDetectorRef =
        inject(ChangeDetectorRef);

    protected portfolios: PortfolioResponse[] = [];

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

    protected dialogMode: PortfolioDialogMode = null;
    protected selectedPortfolio: PortfolioResponse | null = null;

    protected createName = '';
    protected createMethod: PortfolioCreationMethod =
        'BY_AMOUNT';
    protected createInitialValue: number | null = null;

    protected renameValue = '';
    protected currentValue: number | null = null;

    ngOnInit(): void {
        this.loadPortfolios();
    }

    protected retry(): void {
        this.loadPortfolios();
    }

    protected goToPreviousPage(): void {
        if (this.isFirstPage || this.isLoading) {
            return;
        }

        this.currentPage -= 1;
        this.loadPortfolios();
    }

    protected goToNextPage(): void {
        if (this.isLastPage || this.isLoading) {
            return;
        }

        this.currentPage += 1;
        this.loadPortfolios();
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
        portfolio: PortfolioResponse,
    ): void {
        this.clearMessages();
        this.selectedPortfolio = portfolio;
        this.renameValue = portfolio.name;
        this.dialogMode = 'rename';
    }

    protected openValueDialog(
        portfolio: PortfolioResponse,
    ): void {
        this.clearMessages();
        this.selectedPortfolio = portfolio;
        this.currentValue = portfolio.currentValue;
        this.dialogMode = 'value';
    }

    protected openDeleteDialog(
        portfolio: PortfolioResponse,
    ): void {
        this.clearMessages();
        this.selectedPortfolio = portfolio;
        this.dialogMode = 'delete';
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
                'Portfolio name is required.';
            return;
        }

        if (
            this.createInitialValue === null ||
            this.createInitialValue < 0
        ) {
            this.actionErrorMessage =
                'Initial value must be zero or greater.';
            return;
        }

        const request: PortfolioCreateRequest = {
            name: normalizedName,
            creationMethod: this.createMethod,
            initialValue: this.createInitialValue,
        };

        this.startSubmission();

        this.portfolioService
            .createPortfolio(request)
            .subscribe({
                next: () => {
                    this.finishSubmission();
                    this.dialogMode = null;
                    this.selectedPortfolio = null;
                    this.currentPage = 0;
                    this.successMessage =
                        'Portfolio created successfully.';
                    this.changeDetectorRef.markForCheck();
                    this.loadPortfolios(false);
                },
                error: (error: unknown) => {
                    this.finishSubmission();
                    this.actionErrorMessage =
                        this.resolveActionError(
                            error,
                            'create',
                        );
                    this.changeDetectorRef.markForCheck();
                },
            });
    }

    protected submitRename(): void {
        const portfolio = this.selectedPortfolio;
        const normalizedName =
            this.renameValue.trim();

        if (!portfolio) {
            return;
        }

        if (!normalizedName) {
            this.actionErrorMessage =
                'Portfolio name is required.';
            return;
        }

        this.startSubmission();

        this.portfolioService
            .renamePortfolio(
                portfolio.id,
                {
                    name: normalizedName,
                },
            )
            .subscribe({
                next: () => {
                    this.finishSubmission();
                    this.dialogMode = null;
                    this.selectedPortfolio = null;
                    this.successMessage =
                        'Portfolio renamed successfully.';
                    this.changeDetectorRef.markForCheck();
                    this.loadPortfolios(false);
                },
                error: (error: unknown) => {
                    this.finishSubmission();
                    this.actionErrorMessage =
                        this.resolveActionError(
                            error,
                            'rename',
                        );
                    this.changeDetectorRef.markForCheck();
                },
            });
    }

    protected submitValueUpdate(): void {
        const portfolio = this.selectedPortfolio;

        if (!portfolio) {
            return;
        }

        if (
            this.currentValue === null ||
            this.currentValue < 0
        ) {
            this.actionErrorMessage =
                'Current value must be zero or greater.';
            return;
        }

        this.startSubmission();

        this.portfolioService
            .updatePortfolioValue(
                portfolio.id,
                {
                    currentValue: this.currentValue,
                },
            )
            .subscribe({
                next: () => {
                    this.finishSubmission();
                    this.dialogMode = null;
                    this.selectedPortfolio = null;
                    this.successMessage =
                        'Portfolio value updated successfully.';
                    this.changeDetectorRef.markForCheck();
                    this.loadPortfolios(false);
                },
                error: (error: unknown) => {
                    this.finishSubmission();
                    this.actionErrorMessage =
                        this.resolveActionError(
                            error,
                            'update',
                        );
                    this.changeDetectorRef.markForCheck();
                },
            });
    }

    protected submitDelete(): void {
        const portfolio = this.selectedPortfolio;

        if (!portfolio) {
            return;
        }

        this.startSubmission();

        this.portfolioService
            .deletePortfolio(portfolio.id)
            .subscribe({
                next: () => {
                    this.finishSubmission();
                    this.dialogMode = null;
                    this.selectedPortfolio = null;
                    this.successMessage =
                        'Portfolio deleted successfully.';

                    if (
                        this.portfolios.length === 1 &&
                        this.currentPage > 0
                    ) {
                        this.currentPage -= 1;
                    }

                    this.changeDetectorRef.markForCheck();
                    this.loadPortfolios(false);
                },
                error: (error: unknown) => {
                    this.finishSubmission();
                    this.actionErrorMessage =
                        this.resolveActionError(
                            error,
                            'delete',
                        );
                    this.changeDetectorRef.markForCheck();
                },
            });
    }

    protected dismissSuccessMessage(): void {
        this.successMessage = '';
    }

    protected isPositiveOrZero(
        value: number,
    ): boolean {
        return value >= 0;
    }

    protected get visiblePageNumber(): number {
        if (this.totalPages === 0) {
            return 0;
        }

        return this.currentPage + 1;
    }

    protected get totalCurrentValue(): number {
        return this.portfolios.reduce(
            (
                total,
                portfolio,
            ) => total + portfolio.currentValue,
            0,
        );
    }

    protected get displayedProfit(): number {
        return this.portfolios.reduce(
            (
                total,
                portfolio,
            ) =>
                total +
                (
                    portfolio.currentValue -
                    portfolio.initialValue
                ),
            0,
        );
    }

    private loadPortfolios(
        clearMessages = true,
    ): void {
        this.isLoading = true;
        this.errorMessage = '';

        if (clearMessages) {
            this.successMessage = '';
        }

        this.changeDetectorRef.markForCheck();

        this.portfolioService
            .getPortfolios({
                page: this.currentPage,
                size: this.pageSize,
                sortBy: 'createdAt',
                sortDirection: 'desc',
            })
            .subscribe({
                next: (
                    response:
                        PagedResponse<PortfolioResponse>,
                ) => {
                    this.applyResponse(response);
                    this.isLoading = false;
                    this.changeDetectorRef.markForCheck();
                },
                error: (error: unknown) => {
                    this.isLoading = false;
                    this.portfolios = [];
                    this.totalElements = 0;
                    this.totalPages = 0;
                    this.isFirstPage = true;
                    this.isLastPage = true;
                    this.errorMessage =
                        this.resolveLoadError(error);
                    this.changeDetectorRef.markForCheck();
                },
            });
    }

    private applyResponse(
        response: PagedResponse<PortfolioResponse>,
    ): void {
        this.portfolios = response.content;
        this.currentPage = response.page;
        this.pageSize = response.size;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.isFirstPage = response.first;
        this.isLastPage = response.last;
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

    private resolveLoadError(
        error: unknown,
    ): string {
        if (!(error instanceof HttpErrorResponse)) {
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

    private resolveActionError(
        error: unknown,
        action: PortfolioAction,
    ): string {
        if (!(error instanceof HttpErrorResponse)) {
            return this.defaultActionError(action);
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

        if (error.status === 409) {
            if (
                action === 'create' ||
                action === 'rename'
            ) {
                return (
                    'A portfolio with this name ' +
                    'already exists.'
                );
            }

            return (
                'The portfolio conflicts ' +
                'with existing data.'
            );
        }

        if (error.status === 400) {
            return (
                this.extractBackendMessage(error) ??
                'The submitted portfolio information is invalid.'
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
        const responseBody = error.error;

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
        action: PortfolioAction,
    ): string {
        switch (action) {
            case 'create':
                return (
                    'The portfolio could not be created.'
                );

            case 'rename':
                return (
                    'The portfolio could not be renamed.'
                );

            case 'update':
                return (
                    'The portfolio value could not be updated.'
                );

            case 'delete':
                return (
                    'The portfolio could not be deleted.'
                );
        }
    }

    private clearMessages(): void {
        this.actionErrorMessage = '';
        this.successMessage = '';
    }
}