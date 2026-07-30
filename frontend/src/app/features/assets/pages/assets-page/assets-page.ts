import {
    DatePipe,
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
    ASSET_TYPE_OPTIONS,
    AssetCreateRequest,
    AssetResponse,
    AssetType,
    AssetUpdateRequest,
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

type AssetDialogMode =
    | 'create'
    | 'edit'
    | 'delete'
    | null;

type AssetAction =
    | 'create'
    | 'edit'
    | 'delete';

interface AssetFormValue {
    symbol: string;
    displayName: string;
    assetType: AssetType;
    currency: string;
    isin: string;
    exchange: string;
    notes: string;
}

@Component({
    selector: 'app-assets-page',
    imports: [
        DatePipe,
        FormsModule,
    ],
    templateUrl: './assets-page.html',
    styleUrl: './assets-page.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetsPage implements OnInit {
    private readonly assetService =
        inject(AssetService);

    private readonly portfolioService =
        inject(PortfolioService);

    private readonly changeDetectorRef =
        inject(ChangeDetectorRef);

    protected readonly assetTypeOptions =
        ASSET_TYPE_OPTIONS;

    protected portfolios: PortfolioResponse[] = [];
    protected assets: AssetResponse[] = [];
    protected filteredAssets: AssetResponse[] = [];

    protected selectedPortfolioId = '';
    protected searchValue = '';

    protected isLoadingPortfolios = true;
    protected isLoadingAssets = false;
    protected isSubmitting = false;

    protected errorMessage = '';
    protected actionErrorMessage = '';
    protected successMessage = '';

    protected dialogMode: AssetDialogMode = null;
    protected selectedAsset: AssetResponse | null = null;

    protected formValue: AssetFormValue =
        this.createEmptyFormValue();

    ngOnInit(): void {
        this.loadPortfolios();
    }

    protected retry(): void {
        if (this.portfolios.length === 0) {
            this.loadPortfolios();
            return;
        }

        this.loadAssets();
    }

    protected selectPortfolio(
        portfolioId: string,
    ): void {
        if (
            portfolioId === this.selectedPortfolioId ||
            this.isLoadingAssets
        ) {
            return;
        }

        this.selectedPortfolioId = portfolioId;
        this.searchValue = '';
        this.loadAssets();
    }

    protected onSearchChange(): void {
        this.applySearchFilter();
    }

    protected openCreateDialog(): void {
        if (!this.selectedPortfolioId) {
            return;
        }

        this.clearMessages();
        this.selectedAsset = null;
        this.formValue = this.createEmptyFormValue();
        this.dialogMode = 'create';
    }

    protected openEditDialog(
        asset: AssetResponse,
    ): void {
        this.clearMessages();
        this.selectedAsset = asset;

        this.formValue = {
            symbol: asset.symbol,
            displayName: asset.displayName,
            assetType: asset.assetType,
            currency: asset.currency,
            isin: asset.isin ?? '',
            exchange: asset.exchange ?? '',
            notes: asset.notes ?? '',
        };

        this.dialogMode = 'edit';
    }

    protected openDeleteDialog(
        asset: AssetResponse,
    ): void {
        this.clearMessages();
        this.selectedAsset = asset;
        this.dialogMode = 'delete';
    }

    protected closeDialog(): void {
        if (this.isSubmitting) {
            return;
        }

        this.dialogMode = null;
        this.selectedAsset = null;
        this.actionErrorMessage = '';
    }

    protected submitCreate(): void {
        const request = this.buildRequest();

        if (!request) {
            return;
        }

        if (!this.selectedPortfolioId) {
            this.actionErrorMessage =
                'Select a portfolio before creating an asset.';
            return;
        }

        this.startSubmission();

        this.assetService
            .createAsset(
                this.selectedPortfolioId,
                request,
            )
            .subscribe({
                next: () => {
                    this.finishSubmission();
                    this.dialogMode = null;
                    this.selectedAsset = null;
                    this.successMessage =
                        'Asset created successfully.';
                    this.changeDetectorRef.markForCheck();
                    this.loadAssets(false);
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

    protected submitEdit(): void {
        const asset = this.selectedAsset;
        const request = this.buildRequest();

        if (
            !asset ||
            !request ||
            !this.selectedPortfolioId
        ) {
            return;
        }

        this.startSubmission();

        this.assetService
            .updateAsset(
                this.selectedPortfolioId,
                asset.id,
                request,
            )
            .subscribe({
                next: () => {
                    this.finishSubmission();
                    this.dialogMode = null;
                    this.selectedAsset = null;
                    this.successMessage =
                        'Asset updated successfully.';
                    this.changeDetectorRef.markForCheck();
                    this.loadAssets(false);
                },
                error: (error: unknown) => {
                    this.finishSubmission();
                    this.actionErrorMessage =
                        this.resolveActionError(
                            error,
                            'edit',
                        );
                    this.changeDetectorRef.markForCheck();
                },
            });
    }

    protected submitDelete(): void {
        const asset = this.selectedAsset;

        if (
            !asset ||
            !this.selectedPortfolioId
        ) {
            return;
        }

        this.startSubmission();

        this.assetService
            .deleteAsset(
                this.selectedPortfolioId,
                asset.id,
            )
            .subscribe({
                next: () => {
                    this.finishSubmission();
                    this.dialogMode = null;
                    this.selectedAsset = null;
                    this.successMessage =
                        'Asset deleted successfully.';
                    this.changeDetectorRef.markForCheck();
                    this.loadAssets(false);
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

    protected get uniqueCurrencies(): number {
        return new Set(
            this.assets.map(
                (asset) => asset.currency,
            ),
        ).size;
    }

    protected get uniqueAssetTypes(): number {
        return new Set(
            this.assets.map(
                (asset) => asset.assetType,
            ),
        ).size;
    }

    protected getAssetTypeLabel(
        assetType: AssetType,
    ): string {
        return (
            this.assetTypeOptions.find(
                (option) =>
                    option.value === assetType,
            )?.label ?? assetType
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
                    this.portfolios = response.content;
                    this.isLoadingPortfolios = false;

                    if (this.portfolios.length > 0) {
                        this.selectedPortfolioId =
                            this.portfolios[0].id;
                        this.loadAssets();
                    } else {
                        this.selectedPortfolioId = '';
                        this.assets = [];
                        this.filteredAssets = [];
                    }

                    this.changeDetectorRef.markForCheck();
                },
                error: (error: unknown) => {
                    this.isLoadingPortfolios = false;
                    this.portfolios = [];
                    this.assets = [];
                    this.filteredAssets = [];
                    this.selectedPortfolioId = '';
                    this.errorMessage =
                        this.resolvePortfolioLoadError(
                            error,
                        );
                    this.changeDetectorRef.markForCheck();
                },
            });
    }

    private loadAssets(
        clearMessages = true,
    ): void {
        if (!this.selectedPortfolioId) {
            return;
        }

        this.isLoadingAssets = true;
        this.errorMessage = '';

        if (clearMessages) {
            this.successMessage = '';
        }

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
                    this.applySearchFilter();
                    this.isLoadingAssets = false;
                    this.changeDetectorRef.markForCheck();
                },
                error: (error: unknown) => {
                    this.isLoadingAssets = false;
                    this.assets = [];
                    this.filteredAssets = [];
                    this.errorMessage =
                        this.resolveAssetLoadError(
                            error,
                        );
                    this.changeDetectorRef.markForCheck();
                },
            });
    }

    private applySearchFilter(): void {
        const normalizedSearch =
            this.searchValue
                .trim()
                .toLowerCase();

        if (!normalizedSearch) {
            this.filteredAssets = [
                ...this.assets,
            ];
            return;
        }

        this.filteredAssets =
            this.assets.filter(
                (asset) =>
                    asset.symbol
                        .toLowerCase()
                        .includes(normalizedSearch) ||
                    asset.displayName
                        .toLowerCase()
                        .includes(normalizedSearch) ||
                    asset.currency
                        .toLowerCase()
                        .includes(normalizedSearch) ||
                    asset.assetType
                        .toLowerCase()
                        .includes(normalizedSearch) ||
                    (
                        asset.exchange
                            ?.toLowerCase()
                            .includes(normalizedSearch) ??
                        false
                    ),
            );
    }

    private buildRequest():
        AssetCreateRequest | AssetUpdateRequest | null {
        const symbol =
            this.formValue.symbol
                .trim()
                .toUpperCase();

        const displayName =
            this.formValue.displayName.trim();

        const currency =
            this.formValue.currency
                .trim()
                .toUpperCase();

        const isin =
            this.normalizeOptionalValue(
                this.formValue.isin,
                true,
            );

        const exchange =
            this.normalizeOptionalValue(
                this.formValue.exchange,
                false,
            );

        const notes =
            this.normalizeOptionalValue(
                this.formValue.notes,
                false,
            );

        if (!symbol) {
            this.actionErrorMessage =
                'Asset symbol is required.';
            return null;
        }

        if (symbol.length > 30) {
            this.actionErrorMessage =
                'Asset symbol must not exceed 30 characters.';
            return null;
        }

        if (!displayName) {
            this.actionErrorMessage =
                'Asset display name is required.';
            return null;
        }

        if (displayName.length > 160) {
            this.actionErrorMessage =
                'Asset display name must not exceed 160 characters.';
            return null;
        }

        if (!/^[A-Z]{3}$/.test(currency)) {
            this.actionErrorMessage =
                'Currency must contain exactly 3 letters.';
            return null;
        }

        if (
            isin !== null &&
            !/^[A-Z0-9]{12}$/.test(isin)
        ) {
            this.actionErrorMessage =
                'ISIN must contain exactly 12 letters or digits.';
            return null;
        }

        if (
            exchange !== null &&
            exchange.length > 40
        ) {
            this.actionErrorMessage =
                'Exchange must not exceed 40 characters.';
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

        return {
            symbol,
            displayName,
            assetType:
                this.formValue.assetType,
            currency,
            isin,
            exchange,
            notes,
        };
    }

    private normalizeOptionalValue(
        value: string,
        uppercase: boolean,
    ): string | null {
        const normalizedValue = value.trim();

        if (!normalizedValue) {
            return null;
        }

        return uppercase
            ? normalizedValue.toUpperCase()
            : normalizedValue;
    }

    private createEmptyFormValue():
        AssetFormValue {
        return {
            symbol: '',
            displayName: '',
            assetType: 'STOCK',
            currency: 'USD',
            isin: '',
            exchange: '',
            notes: '',
        };
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

    private resolveAssetLoadError(
        error: unknown,
    ): string {
        if (!(error instanceof HttpErrorResponse)) {
            return (
                'Assets could not be loaded. ' +
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
                'to view assets.'
            );
        }

        if (error.status === 404) {
            return (
                'The selected portfolio ' +
                'could not be found.'
            );
        }

        return (
            'Assets could not be loaded. ' +
            'Please try again.'
        );
    }

    private resolveActionError(
        error: unknown,
        action: AssetAction,
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

        if (error.status === 404) {
            return (
                'The asset or portfolio ' +
                'could not be found.'
            );
        }

        if (error.status === 409) {
            return (
                'An asset with this symbol ' +
                'already exists in the portfolio.'
            );
        }

        if (error.status === 400) {
            return (
                this.extractBackendMessage(error) ??
                'The submitted asset information is invalid.'
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
        action: AssetAction,
    ): string {
        switch (action) {
            case 'create':
                return (
                    'The asset could not be created.'
                );

            case 'edit':
                return (
                    'The asset could not be updated.'
                );

            case 'delete':
                return (
                    'The asset could not be deleted.'
                );
        }
    }

    private clearMessages(): void {
        this.actionErrorMessage = '';
        this.successMessage = '';
    }
}