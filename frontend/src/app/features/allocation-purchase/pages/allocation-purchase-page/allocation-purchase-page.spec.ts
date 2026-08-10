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
            },
        );

        it(
            'should allow a zero percent target',
            () => {
                initializeSelection();

                component.targetWeightPercent =
                    0;

                expect(
                    component.canPreview,
                ).toBe(true);

                component.requestPreview();

                expect(
                    previewPurchase,
                ).toHaveBeenCalledWith(
                    amountPortfolio.id,
                    {
                        assetId:
                            selectedAsset.id,
                        targetWeightPercent:
                            0,
                    },
                );
            },
        );

        it(
            'should display a buy preview',
            () => {
                initializeSelection();

                component.targetWeightPercent =
                    12.5;

                component.requestPreview();

                const preview =
                    createBuyPreviewResponse();

                previewSubject.next(preview);

                fixture.detectChanges();

                expect(
                    component.preview,
                ).toEqual(preview);

                expect(
                    component.canExecute,
                ).toBe(true);
            },
        );

        it(
            'should display and execute a sell preview',
            () => {
                initializeSelection();

                component.targetWeightPercent =
                    10;

                component.fee =
                    1;

                component.requestPreview();

                previewSubject.next(
                    createSellPreviewResponse(),
                );

                expect(
                    component.canExecute,
                ).toBe(true);

                component.executePurchase();

                expect(
                    executePurchase,
                ).toHaveBeenCalledWith(
                    amountPortfolio.id,
                    {
                        assetId:
                            selectedAsset.id,
                        targetWeightPercent:
                            10,
                        fee:
                            1,
                    },
                );

                executionSubject.next(
                    createSellExecutionResponse(),
                );

                fixture.detectChanges();

                expect(
                    component.successMessage,
                ).toContain(
                    'Sell 5 TTWO successfully',
                );
            },
        );

        it(
            'should not execute a hold preview',
            () => {
                initializeSelection();

                component.targetWeightPercent =
                    20;

                component.requestPreview();

                previewSubject.next(
                    createHoldPreviewResponse(),
                );

                expect(
                    component.canExecute,
                ).toBe(false);

                component.executePurchase();

                expect(
                    executePurchase,
                ).not.toHaveBeenCalled();
            },
        );

        it(
            'should allow temporary target allocation above 100 percent',
            () => {
                initializeSelection();

                component.targetWeightPercent =
                    80;

                component.requestPreview();

                const preview = {
                    ...createBuyPreviewResponse(),
                    targetWeightPercent:
                        80,
                    totalTargetWeightPercent:
                        125,
                    allocationDifferencePercent:
                        -25,
                } satisfies
                    AllocationPurchasePreviewResponse;

                previewSubject.next(preview);

                fixture.detectChanges();

                expect(
                    component.preview
                        ?.totalTargetWeightPercent,
                ).toBe(125);

                expect(
                    component.errorMessage,
                ).toBe('');
            },
        );

        it(
            'should reject a target above 100 percent',
            () => {
                initializeSelection();

                component.targetWeightPercent =
                    101;

                component.requestPreview();

                expect(
                    previewPurchase,
                ).not.toHaveBeenCalled();

                expect(
                    component.errorMessage,
                ).toBeTruthy();
            },
        );

        it(
            'should execute a buy allocation trade',
            () => {
                initializeSelection();

                component.targetWeightPercent =
                    12.5;

                component.fee =
                    1.25;

                component.requestPreview();

                previewSubject.next(
                    createBuyPreviewResponse(),
                );

                component.executePurchase();

                expect(
                    component.isExecuting,
                ).toBe(true);

                executionSubject.next(
                    createBuyExecutionResponse(),
                );

                fixture.detectChanges();

                expect(
                    component.execution,
                ).not.toBeNull();

                expect(
                    component.isExecuting,
                ).toBe(false);

                expect(
                    component.successMessage,
                ).toContain(
                    'Buy 6.08124543 TTWO successfully',
                );
            },
        );

        it(
            'should display backend error when preview fails',
            () => {
                initializeSelection();

                component.targetWeightPercent =
                    50;

                component.requestPreview();

                previewSubject.error(
                    new HttpErrorResponse({
                        status: 400,
                        statusText:
                            'Bad Request',
                        error: {
                            message:
                                'Preview failed',
                        },
                    }),
                );

                fixture.detectChanges();

                expect(
                    component.preview,
                ).toBeNull();

                expect(
                    component.errorMessage,
                ).toContain(
                    'Preview failed',
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
                    createBuyPreviewResponse(),
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

        function createBuyPreviewResponse():
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
                portfolioCurrentValue:
                    10000,
                currentPrice:
                    205.55,
                targetWeightPercent:
                    12.5,
                weightAssignedToOtherAssetsPercent:
                    20,
                totalTargetWeightPercent:
                    32.5,
                allocationDifferencePercent:
                    67.5,
                currentMarketValue:
                    0,
                targetMarketValue:
                    1250,
                existingQuantity:
                    0,
                targetQuantity:
                    6.08124543,
                quantityDifference:
                    6.08124543,
                quantityToBuy:
                    6.08124543,
                quantityToSell:
                    0,
                estimatedTradeAmount:
                    1249.99999814,
                suggestedAction:
                    'BUY',
                calculatedAt:
                    '2026-08-10T10:00:00Z',
            };
        }

        function createSellPreviewResponse():
            AllocationPurchasePreviewResponse {
            return {
                ...createBuyPreviewResponse(),
                targetWeightPercent:
                    10,
                currentMarketValue:
                    2027.5,
                targetMarketValue:
                    1000,
                existingQuantity:
                    10,
                targetQuantity:
                    5,
                quantityDifference:
                    -5,
                quantityToBuy:
                    0,
                quantityToSell:
                    5,
                estimatedTradeAmount:
                    1027.75,
                suggestedAction:
                    'SELL',
            };
        }

        function createHoldPreviewResponse():
            AllocationPurchasePreviewResponse {
            return {
                ...createBuyPreviewResponse(),
                targetWeightPercent:
                    20,
                currentMarketValue:
                    2000,
                targetMarketValue:
                    2000,
                existingQuantity:
                    10,
                targetQuantity:
                    10,
                quantityDifference:
                    0,
                quantityToBuy:
                    0,
                quantityToSell:
                    0,
                estimatedTradeAmount:
                    0,
                suggestedAction:
                    'HOLD',
            };
        }

        function createBuyExecutionResponse():
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
                action:
                    'BUY',
                targetWeightPercent:
                    12.5,
                unitPrice:
                    205.55,
                tradedQuantity:
                    6.08124543,
                tradeAmount:
                    1249.99999814,
                fee:
                    1.25,
                cashImpact:
                    -1251.24999814,
                availableCashBefore:
                    10000,
                availableCashAfter:
                    8748.75000186,
                totalTargetWeightPercent:
                    32.5,
                allocationDifferencePercent:
                    67.5,
                currency:
                    'USD',
                executedAt:
                    '2026-08-10T10:05:00Z',
            };
        }

        function createSellExecutionResponse():
            AllocationPurchaseExecutionResponse {
            return {
                ...createBuyExecutionResponse(),
                action:
                    'SELL',
                targetWeightPercent:
                    10,
                tradedQuantity:
                    5,
                tradeAmount:
                    1027.75,
                fee:
                    1,
                cashImpact:
                    1026.75,
                availableCashBefore:
                    5000,
                availableCashAfter:
                    6026.75,
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
            '2026-08-10T09:00:00Z',
        updatedAt:
            '2026-08-10T09:00:00Z',
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
            '2026-08-10T09:00:00Z',
        updatedAt:
            '2026-08-10T09:00:00Z',
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