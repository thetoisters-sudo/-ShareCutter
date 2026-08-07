import {
    ComponentFixture,
    TestBed,
} from '@angular/core/testing';
import {
    HttpErrorResponse,
} from '@angular/common/http';
import {
    Subject,
} from 'rxjs';
import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest';

import {
    AllocationPurchaseExecutionResponse,
    AllocationPurchasePreviewResponse,
} from '../../../../core/allocation-purchase/models/allocation-purchase.models';
import {
    AllocationPurchaseService,
} from '../../../../core/allocation-purchase/services/allocation-purchase.service';
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
    AllocationPurchasePage,
} from './allocation-purchase-page';

interface TestableAllocationPurchasePage {
    portfolios: PortfolioResponse[];
    assets: AssetResponse[];
    selectedPortfolioId: string;
    selectedAssetId: string;
    targetWeightPercent: number | null;
    fee: number | null;
    preview:
    AllocationPurchasePreviewResponse | null;
    execution:
    AllocationPurchaseExecutionResponse | null;
    isLoadingPortfolios: boolean;
    isLoadingAssets: boolean;
    isPreviewing: boolean;
    isExecuting: boolean;
    errorMessage: string;
    successMessage: string;
    canPreview: boolean;
    canExecute: boolean;

    selectPortfolio(
        portfolioId: string,
    ): void;

    selectAsset(
        assetId: string,
    ): void;

    requestPreview(): void;

    executePurchase(): void;

    clearResult(): void;
}

describe(
    'AllocationPurchasePage',
    () => {
        const amountPortfolio =
            createPortfolio(
                'portfolio-by-amount',
                'Growth Portfolio',
                'BY_AMOUNT',
            );

        const secondAmountPortfolio =
            createPortfolio(
                'portfolio-by-amount-2',
                'Income Portfolio',
                'BY_AMOUNT',
            );

        const holdingsPortfolio =
            createPortfolio(
                'portfolio-by-holdings',
                'Imported Portfolio',
                'BY_HOLDINGS',
            );

        const selectedAsset =
            createAsset(
                'asset-ttwo',
                amountPortfolio.id,
                'TTWO',
            );

        const secondAsset =
            createAsset(
                'asset-msft',
                secondAmountPortfolio.id,
                'MSFT',
            );

        let fixture:
            ComponentFixture<AllocationPurchasePage>;

        let component:
            TestableAllocationPurchasePage;

        let portfoliosSubject:
            Subject<
                PagedResponse<PortfolioResponse>
            >;

        let assetsSubject:
            Subject<AssetResponse[]>;

        let previewSubject:
            Subject<
                AllocationPurchasePreviewResponse
            >;

        let executionSubject:
            Subject<
                AllocationPurchaseExecutionResponse
            >;

        const getPortfolios =
            vi.fn();

        const getAssets =
            vi.fn();

        const previewPurchase =
            vi.fn();

        const executePurchase =
            vi.fn();

        const notifyPortfolioChanged =
            vi.fn();

        beforeEach(async () => {
            portfoliosSubject =
                new Subject<
                    PagedResponse<PortfolioResponse>
                >();

            assetsSubject =
                new Subject<AssetResponse[]>();

            previewSubject =
                new Subject<
                    AllocationPurchasePreviewResponse
                >();

            executionSubject =
                new Subject<
                    AllocationPurchaseExecutionResponse
                >();

            getPortfolios.mockReset();
            getAssets.mockReset();
            previewPurchase.mockReset();
            executePurchase.mockReset();
            notifyPortfolioChanged.mockReset();

            getPortfolios.mockReturnValue(
                portfoliosSubject.asObservable(),
            );

            getAssets.mockReturnValue(
                assetsSubject.asObservable(),
            );

            previewPurchase.mockReturnValue(
                previewSubject.asObservable(),
            );

            executePurchase.mockReturnValue(
                executionSubject.asObservable(),
            );

            await TestBed.configureTestingModule({
                imports: [
                    AllocationPurchasePage,
                ],
                providers: [
                    {
                        provide:
                            PortfolioService,
                        useValue: {
                            getPortfolios,
                            notifyPortfolioChanged,
                        },
                    },
                    {
                        provide:
                            AssetService,
                        useValue: {
                            getAssets,
                        },
                    },
                    {
                        provide:
                            AllocationPurchaseService,
                        useValue: {
                            previewPurchase,
                            executePurchase,
                        },
                    },
                ],
            }).compileComponents();

            fixture =
                TestBed.createComponent(
                    AllocationPurchasePage,
                );

            component =
                fixture.componentInstance as unknown as
                TestableAllocationPurchasePage;

            fixture.detectChanges();
        });

        it(
            'should create and request portfolios on initialization',
            () => {
                expect(
                    fixture.componentInstance,
                ).toBeTruthy();

                expect(
                    component.isLoadingPortfolios,
                ).toBe(true);

                expect(
                    getPortfolios,
                ).toHaveBeenCalledTimes(1);

                expect(
                    getPortfolios,
                ).toHaveBeenCalledWith({
                    page: 0,
                    size: 100,
                    sortBy: 'createdAt',
                    sortDirection: 'desc',
                });
            },
        );

        it(
            'should keep only portfolios created by amount',
            () => {
                portfoliosSubject.next(
                    createPortfolioPage([
                        amountPortfolio,
                        holdingsPortfolio,
                        secondAmountPortfolio,
                    ]),
                );

                fixture.detectChanges();

                expect(
                    component.portfolios,
                ).toEqual([
                    amountPortfolio,
                    secondAmountPortfolio,
                ]);

                expect(
                    component.selectedPortfolioId,
                ).toBe(
                    amountPortfolio.id,
                );

                expect(
                    getAssets,
                ).toHaveBeenCalledWith(
                    amountPortfolio.id,
                );
            },
        );

        it(
            'should load USD assets for the selected portfolio',
            () => {
                const eurAsset = {
                    ...createAsset(
                        'asset-eur',
                        amountPortfolio.id,
                        'SAP',
                    ),
                    currency: 'EUR',
                } satisfies AssetResponse;

                portfoliosSubject.next(
                    createPortfolioPage([
                        amountPortfolio,
                    ]),
                );

                assetsSubject.next([
                    selectedAsset,
                    eurAsset,
                ]);

                fixture.detectChanges();

                expect(
                    component.assets,
                ).toEqual([
                    selectedAsset,
                ]);

                expect(
                    component.selectedAssetId,
                ).toBe(
                    selectedAsset.id,
                );

                expect(
                    component.isLoadingAssets,
                ).toBe(false);
            },
        );

        it(
            'should request and display a purchase preview',
            () => {
                initializeSelection();

                component.targetWeightPercent =
                    12.5;

                component.fee =
                    1.25;

                component.requestPreview();

                expect(
                    component.isPreviewing,
                ).toBe(true);

                expect(
                    previewPurchase,
                ).toHaveBeenCalledWith(
                    amountPortfolio.id,
                    {
                        assetId:
                            selectedAsset.id,
                        targetWeightPercent:
                            12.5,
                    },
                );

                const preview =
                    createPreviewResponse();

                previewSubject.next(preview);

                fixture.detectChanges();

                expect(
                    component.preview,
                ).toEqual(preview);

                expect(
                    component.isPreviewing,
                ).toBe(false);

                expect(
                    component.canExecute,
                ).toBe(true);

                const compiled =
                    fixture.nativeElement as HTMLElement;

                expect(
                    compiled.textContent,
                ).toContain(
                    'Purchase preview',
                );

                expect(
                    compiled.textContent,
                ).toContain('TTWO');
            },
        );

        it(
            'should reject preview when the target weight is invalid',
            () => {
                initializeSelection();

                component.targetWeightPercent =
                    0;

                component.requestPreview();

                expect(
                    previewPurchase,
                ).not.toHaveBeenCalled();

                expect(
                    component.errorMessage,
                ).toContain(
                    'Target weight must be greater than zero',
                );
            },
        );

        it(
            'should not execute without a calculated preview',
            () => {
                initializeSelection();

                component.targetWeightPercent =
                    12.5;

                component.executePurchase();

                expect(
                    executePurchase,
                ).not.toHaveBeenCalled();

                expect(
                    component.errorMessage,
                ).toContain(
                    'Calculate a preview before executing',
                );
            },
        );

        it(
            'should execute the previewed allocation purchase',
            () => {
                initializeSelection();

                component.targetWeightPercent =
                    12.5;

                component.fee =
                    1.25;

                component.requestPreview();

                previewSubject.next(
                    createPreviewResponse(),
                );

                component.executePurchase();

                expect(
                    component.isExecuting,
                ).toBe(true);

                expect(
                    executePurchase,
                ).toHaveBeenCalledWith(
                    amountPortfolio.id,
                    {
                        assetId:
                            selectedAsset.id,
                        targetWeightPercent:
                            12.5,
                        fee:
                            1.25,
                    },
                );

                const execution =
                    createExecutionResponse();

                executionSubject.next(
                    execution,
                );

                fixture.detectChanges();

                expect(
                    component.execution,
                ).toEqual(execution);

                expect(
                    component.preview,
                ).toBeNull();

                expect(
                    component.isExecuting,
                ).toBe(false);

                expect(
                    component.successMessage,
                ).toContain(
                    'Purchased 6.08124543 TTWO successfully',
                );

                const compiled =
                    fixture.nativeElement as HTMLElement;

                expect(
                    compiled.textContent,
                ).toContain(
                    'Purchase completed',
                );

                expect(
                    compiled.textContent,
                ).toContain(
                    execution.transactionId,
                );
            },
        );

        it(
            'should display backend error when preview fails',
            () => {
                initializeSelection();

                component.targetWeightPercent =
                    90;

                component.requestPreview();

                previewSubject.error(
                    new HttpErrorResponse({
                        status: 400,
                        statusText: 'Bad Request',
                        error: {
                            message:
                                (
                                    'Requested target weight exceeds ' +
                                    'the remaining portfolio allocation.'
                                ),
                        },
                        url:
                            (
                                '/api/v1/portfolios/' +
                                amountPortfolio.id +
                                '/allocation-purchases/preview'
                            ),
                    }),
                );

                fixture.detectChanges();

                expect(
                    component.preview,
                ).toBeNull();

                expect(
                    component.isPreviewing,
                ).toBe(false);

                expect(
                    component.errorMessage,
                ).toContain(
                    'Requested target weight exceeds',
                );
            },
        );

        it(
            'should clear asset and preview when changing portfolio',
            () => {
                initializeSelection();

                component.targetWeightPercent =
                    12.5;

                component.requestPreview();

                previewSubject.next(
                    createPreviewResponse(),
                );

                const nextAssetsSubject =
                    new Subject<
                        AssetResponse[]
                    >();

                getAssets.mockReturnValueOnce(
                    nextAssetsSubject.asObservable(),
                );

                component.selectPortfolio(
                    secondAmountPortfolio.id,
                );

                expect(
                    component.selectedPortfolioId,
                ).toBe(
                    secondAmountPortfolio.id,
                );

                expect(
                    component.selectedAssetId,
                ).toBe('');

                expect(
                    component.preview,
                ).toBeNull();

                expect(
                    component.execution,
                ).toBeNull();

                expect(
                    component.targetWeightPercent,
                ).toBeNull();

                expect(
                    getAssets,
                ).toHaveBeenCalledWith(
                    secondAmountPortfolio.id,
                );

                nextAssetsSubject.next([
                    secondAsset,
                ]);

                expect(
                    component.selectedAssetId,
                ).toBe(
                    secondAsset.id,
                );
            },
        );

        it(
            'should reset preview, execution, fee and target weight',
            () => {
                initializeSelection();

                component.targetWeightPercent =
                    12.5;

                component.fee =
                    1.25;

                component.requestPreview();

                previewSubject.next(
                    createPreviewResponse(),
                );

                component.clearResult();

                expect(
                    component.preview,
                ).toBeNull();

                expect(
                    component.execution,
                ).toBeNull();

                expect(
                    component.targetWeightPercent,
                ).toBeNull();

                expect(
                    component.fee,
                ).toBeNull();

                expect(
                    component.errorMessage,
                ).toBe('');

                expect(
                    component.successMessage,
                ).toBe('');
            },
        );

        function initializeSelection(): void {
            portfoliosSubject.next(
                createPortfolioPage([
                    amountPortfolio,
                ]),
            );

            assetsSubject.next([
                selectedAsset,
            ]);

            fixture.detectChanges();
        }

        function createPreviewResponse():
            AllocationPurchasePreviewResponse {
            return {
                portfolioId:
                    amountPortfolio.id,
                portfolioName:
                    amountPortfolio.name,
                assetId:
                    selectedAsset.id,
                symbol:
                    selectedAsset.symbol,
                displayName:
                    selectedAsset.displayName,
                exchange:
                    selectedAsset.exchange,
                currency:
                    selectedAsset.currency,
                portfolioInitialValue:
                    10000,
                currentPrice:
                    205.55,
                targetWeightPercent:
                    12.5,
                currentlyAssignedWeightPercent:
                    0,
                remainingAssignableWeightPercent:
                    87.5,
                targetMarketValue:
                    1250,
                existingQuantity:
                    0,
                targetQuantity:
                    6.08124543,
                quantityToBuy:
                    6.08124543,
                estimatedPurchaseAmount:
                    1249.99999814,
                suggestedAction:
                    'BUY',
                calculatedAt:
                    '2026-08-06T07:00:00Z',
            };
        }

        function createExecutionResponse():
            AllocationPurchaseExecutionResponse {
            return {
                portfolioId:
                    amountPortfolio.id,
                portfolioName:
                    amountPortfolio.name,
                assetId:
                    selectedAsset.id,
                symbol:
                    selectedAsset.symbol,
                displayName:
                    selectedAsset.displayName,
                transactionId:
                    (
                        '33333333-3333-3333-' +
                        '3333-333333333333'
                    ),
                targetWeightPercent:
                    12.5,
                unitPrice:
                    205.55,
                purchasedQuantity:
                    6.08124543,
                purchaseAmount:
                    1249.99999814,
                fee:
                    1.25,
                totalCashUsed:
                    1251.24999814,
                availableCashBefore:
                    10000,
                availableCashAfter:
                    8748.75000186,
                remainingAssignableWeightPercent:
                    87.5,
                currency:
                    'USD',
                executedAt:
                    '2026-08-06T07:05:00Z',
            };
        }
    },
);

function createPortfolio(
    id: string,
    name: string,
    creationMethod:
        PortfolioResponse['creationMethod'],
): PortfolioResponse {
    return {
        id,
        userId:
            'user-1',
        name,
        creationMethod,
        initialValue:
            10000,
        currentValue:
            10000,
        totalRealizedProfit:
            0,
        totalUnrealizedProfit:
            0,
        totalReturnPercent:
            0,
        createdAt:
            '2026-08-06T06:00:00Z',
        updatedAt:
            '2026-08-06T06:00:00Z',
    };
}

function createAsset(
    id: string,
    portfolioId: string,
    symbol: string,
): AssetResponse {
    return {
        id,
        portfolioId,
        symbol,
        displayName:
            `${symbol} Corporation`,
        assetType:
            'STOCK',
        currency:
            'USD',
        isin:
            null,
        exchange:
            'NASDAQ',
        notes:
            null,
        createdAt:
            '2026-08-06T06:00:00Z',
        updatedAt:
            '2026-08-06T06:00:00Z',
    };
}

function createPortfolioPage(
    portfolios: PortfolioResponse[],
): PagedResponse<PortfolioResponse> {
    return {
        content:
            portfolios,
        page:
            0,
        size:
            100,
        totalElements:
            portfolios.length,
        totalPages:
            portfolios.length > 0
                ? 1
                : 0,
        first:
            true,
        last:
            true,
        hasNext:
            false,
        hasPrevious:
            false,
    };
}

