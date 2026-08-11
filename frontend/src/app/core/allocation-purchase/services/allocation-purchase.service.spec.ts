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
    afterEach,
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest';

import {
    environment,
} from '../../../../environments/environment';
import {
    PortfolioService,
} from '../../portfolio/services/portfolio.service';
import {
    AllocationPurchaseExecuteRequest,
    AllocationPurchaseExecutionResponse,
    AllocationPurchasePreviewRequest,
    AllocationPurchasePreviewResponse,
} from '../models/allocation-purchase.models';
import {
    AllocationPurchaseService,
} from './allocation-purchase.service';

describe(
    'AllocationPurchaseService',
    () => {
        let service:
            AllocationPurchaseService;

        let httpTestingController:
            HttpTestingController;

        const notifyPortfolioChanged =
            vi.fn();

        const portfolioId =
            '11111111-1111-1111-1111-111111111111';

        const assetId =
            '22222222-2222-2222-2222-222222222222';

        const transactionId =
            '33333333-3333-3333-3333-333333333333';

        const baseUrl =
            (
                `${environment.apiBaseUrl}` +
                `/portfolios/${portfolioId}` +
                '/allocation-purchases'
            );

        beforeEach(() => {
            notifyPortfolioChanged.mockReset();

            TestBed.configureTestingModule({
                providers: [
                    AllocationPurchaseService,
                    provideHttpClient(),
                    provideHttpClientTesting(),
                    {
                        provide:
                            PortfolioService,
                        useValue: {
                            notifyPortfolioChanged,
                        },
                    },
                ],
            });

            service =
                TestBed.inject(
                    AllocationPurchaseService,
                );

            httpTestingController =
                TestBed.inject(
                    HttpTestingController,
                );
        });

        afterEach(() => {
            httpTestingController.verify();
        });

        it(
            'should request allocation preview',
            () => {
                const request:
                    AllocationPurchasePreviewRequest = {
                    assetId,
                    targetWeightPercent:
                        12.5,
                };

                const response:
                    AllocationPurchasePreviewResponse = {
                    portfolioId,
                    portfolioName:
                        'Main Portfolio',
                    assetId,
                    symbol:
                        'TTWO',
                    displayName:
                        (
                            'Take-Two Interactive ' +
                            'Software, Inc.'
                        ),
                    exchange:
                        'NASDAQ',
                    currency:
                        'USD',
                    portfolioCurrentValue:
                        10000,
                    currentPrice:
                        205.55,
                    targetWeightPercent:
                        12.5,
                    weightAssignedToOtherAssetsPercent:
                        25,
                    totalTargetWeightPercent:
                        37.5,
                    allocationDifferencePercent:
                        62.5,
                    currentMarketValue:
                        411.1,
                    targetMarketValue:
                        1250,
                    existingQuantity:
                        2,
                    targetQuantity:
                        6.08124543,
                    quantityDifference:
                        4.08124543,
                    quantityToBuy:
                        4.08124543,
                    quantityToSell:
                        0,
                    estimatedTradeAmount:
                        838.89999814,
                    suggestedAction:
                        'BUY',
                    calculatedAt:
                        (
                            '2026-08-10' +
                            'T06:00:00Z'
                        ),
                };

                let receivedResponse:
                    AllocationPurchasePreviewResponse
                    | undefined;

                service
                    .previewPurchase(
                        portfolioId,
                        request,
                    )
                    .subscribe(
                        (result) => {
                            receivedResponse =
                                result;
                        },
                    );

                const httpRequest =
                    httpTestingController.expectOne(
                        `${baseUrl}/preview`,
                    );

                expect(
                    httpRequest.request.method,
                ).toBe('POST');

                expect(
                    httpRequest.request.body,
                ).toEqual(request);

                httpRequest.flush(response);

                expect(
                    receivedResponse,
                ).toEqual(response);

                expect(
                    notifyPortfolioChanged,
                ).not.toHaveBeenCalled();
            },
        );

        it(
            'should execute allocation buy',
            () => {
                const request:
                    AllocationPurchaseExecuteRequest = {
                    assetId,
                    targetWeightPercent:
                        12.5,
                    fee:
                        1.25,
                };

                const response:
                    AllocationPurchaseExecutionResponse = {
                    portfolioId,
                    portfolioName:
                        'Main Portfolio',
                    assetId,
                    symbol:
                        'TTWO',
                    displayName:
                        (
                            'Take-Two Interactive ' +
                            'Software, Inc.'
                        ),
                    transactionId,
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
                        37.5,
                    allocationDifferencePercent:
                        62.5,
                    currency:
                        'USD',
                    executedAt:
                        (
                            '2026-08-10' +
                            'T06:05:00Z'
                        ),
                };

                let receivedResponse:
                    AllocationPurchaseExecutionResponse
                    | undefined;

                service
                    .executePurchase(
                        portfolioId,
                        request,
                    )
                    .subscribe(
                        (result) => {
                            receivedResponse =
                                result;
                        },
                    );

                const httpRequest =
                    httpTestingController.expectOne(
                        `${baseUrl}/execute`,
                    );

                expect(
                    httpRequest.request.method,
                ).toBe('POST');

                expect(
                    httpRequest.request.body,
                ).toEqual(request);

                httpRequest.flush(response);

                expect(
                    receivedResponse,
                ).toEqual(response);

                expect(
                    notifyPortfolioChanged,
                ).toHaveBeenCalledTimes(1);

                expect(
                    notifyPortfolioChanged,
                ).toHaveBeenCalledWith({
                    portfolioId,
                    reason:
                        'transaction-created',
                });
            },
        );

        it(
            'should execute allocation sell',
            () => {
                const request:
                    AllocationPurchaseExecuteRequest = {
                    assetId,
                    targetWeightPercent:
                        10,
                    fee:
                        1,
                };

                const response:
                    AllocationPurchaseExecutionResponse = {
                    portfolioId,
                    portfolioName:
                        'Main Portfolio',
                    assetId,
                    symbol:
                        'TTWO',
                    displayName:
                        (
                            'Take-Two Interactive ' +
                            'Software, Inc.'
                        ),
                    transactionId,
                    action:
                        'SELL',
                    targetWeightPercent:
                        10,
                    unitPrice:
                        205.55,
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
                    totalTargetWeightPercent:
                        95,
                    allocationDifferencePercent:
                        5,
                    currency:
                        'USD',
                    executedAt:
                        (
                            '2026-08-10' +
                            'T06:08:00Z'
                        ),
                };

                let receivedResponse:
                    AllocationPurchaseExecutionResponse
                    | undefined;

                service
                    .executePurchase(
                        portfolioId,
                        request,
                    )
                    .subscribe(
                        (result) => {
                            receivedResponse =
                                result;
                        },
                    );

                const httpRequest =
                    httpTestingController.expectOne(
                        `${baseUrl}/execute`,
                    );

                expect(
                    httpRequest.request.method,
                ).toBe('POST');

                expect(
                    httpRequest.request.body,
                ).toEqual(request);

                httpRequest.flush(response);

                expect(
                    receivedResponse,
                ).toEqual(response);

                expect(
                    notifyPortfolioChanged,
                ).toHaveBeenCalledTimes(1);
            },
        );

        it(
            'should allow null fee when executing trade',
            () => {
                const request:
                    AllocationPurchaseExecuteRequest = {
                    assetId,
                    targetWeightPercent:
                        10,
                    fee:
                        null,
                };

                const response:
                    AllocationPurchaseExecutionResponse = {
                    portfolioId,
                    portfolioName:
                        'Main Portfolio',
                    assetId,
                    symbol:
                        'TTWO',
                    displayName:
                        (
                            'Take-Two Interactive ' +
                            'Software, Inc.'
                        ),
                    transactionId,
                    action:
                        'BUY',
                    targetWeightPercent:
                        10,
                    unitPrice:
                        205.55,
                    tradedQuantity:
                        4.86499635,
                    tradeAmount:
                        999.99999974,
                    fee:
                        0,
                    cashImpact:
                        -999.99999974,
                    availableCashBefore:
                        10000,
                    availableCashAfter:
                        9000.00000026,
                    totalTargetWeightPercent:
                        35,
                    allocationDifferencePercent:
                        65,
                    currency:
                        'USD',
                    executedAt:
                        (
                            '2026-08-10' +
                            'T06:10:00Z'
                        ),
                };

                service
                    .executePurchase(
                        portfolioId,
                        request,
                    )
                    .subscribe();

                const httpRequest =
                    httpTestingController.expectOne(
                        `${baseUrl}/execute`,
                    );

                expect(
                    httpRequest.request.body,
                ).toEqual(request);

                httpRequest.flush(response);

                expect(
                    notifyPortfolioChanged,
                ).toHaveBeenCalledTimes(1);
            },
        );

        it(
            'should support zero percent target preview',
            () => {
                const request:
                    AllocationPurchasePreviewRequest = {
                    assetId,
                    targetWeightPercent:
                        0,
                };

                const response:
                    AllocationPurchasePreviewResponse = {
                    portfolioId,
                    portfolioName:
                        'Main Portfolio',
                    assetId,
                    symbol:
                        'TTWO',
                    displayName:
                        (
                            'Take-Two Interactive ' +
                            'Software, Inc.'
                        ),
                    exchange:
                        'NASDAQ',
                    currency:
                        'USD',
                    portfolioCurrentValue:
                        10000,
                    currentPrice:
                        205.55,
                    targetWeightPercent:
                        0,
                    weightAssignedToOtherAssetsPercent:
                        75,
                    totalTargetWeightPercent:
                        75,
                    allocationDifferencePercent:
                        25,
                    currentMarketValue:
                        2055.5,
                    targetMarketValue:
                        0,
                    existingQuantity:
                        10,
                    targetQuantity:
                        0,
                    quantityDifference:
                        -10,
                    quantityToBuy:
                        0,
                    quantityToSell:
                        10,
                    estimatedTradeAmount:
                        2055.5,
                    suggestedAction:
                        'SELL',
                    calculatedAt:
                        (
                            '2026-08-10' +
                            'T06:12:00Z'
                        ),
                };

                service
                    .previewPurchase(
                        portfolioId,
                        request,
                    )
                    .subscribe();

                const httpRequest =
                    httpTestingController.expectOne(
                        `${baseUrl}/preview`,
                    );

                expect(
                    httpRequest.request.body,
                ).toEqual(request);

                httpRequest.flush(response);

                expect(
                    notifyPortfolioChanged,
                ).not.toHaveBeenCalled();
            },
        );

        it(
            'should not notify portfolio change when execution fails',
            () => {
                const request:
                    AllocationPurchaseExecuteRequest = {
                    assetId,
                    targetWeightPercent:
                        12.5,
                    fee:
                        1.25,
                };

                service
                    .executePurchase(
                        portfolioId,
                        request,
                    )
                    .subscribe({
                        error: () => {
                            return;
                        },
                    });

                const httpRequest =
                    httpTestingController.expectOne(
                        `${baseUrl}/execute`,
                    );

                httpRequest.flush(
                    {
                        message:
                            'Insufficient portfolio cash',
                    },
                    {
                        status:
                            400,
                        statusText:
                            'Bad Request',
                    },
                );

                expect(
                    notifyPortfolioChanged,
                ).not.toHaveBeenCalled();
            },
        );
    },
);