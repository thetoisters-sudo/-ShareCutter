import {
    HttpTestingController,
    provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import {
    PagedResponse,
    PortfolioCreateRequest,
    PortfolioRenameRequest,
    PortfolioResponse,
    PortfolioSummaryResponse,
    PortfolioValueUpdateRequest,
} from '../models/portfolio.models';
import { PortfolioService } from './portfolio.service';

describe('PortfolioService', () => {
    let service: PortfolioService;
    let httpTestingController: HttpTestingController;

    const portfoliosUrl =
        `${environment.apiBaseUrl}/portfolios`;

    const portfolio: PortfolioResponse = {
        id: 'portfolio-1',
        userId: 'user-1',
        name: 'Growth Portfolio',
        creationMethod: 'BY_AMOUNT',
        initialValue: 10000,
        currentValue: 11250,
        totalRealizedProfit: 350,
        totalUnrealizedProfit: 900,
        totalReturnPercent: 12.5,
        createdAt: '2026-07-29T18:00:00Z',
        updatedAt: '2026-07-29T19:00:00Z',
    };

    const pagedResponse:
        PagedResponse<PortfolioResponse> = {
        content: [portfolio],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
    };

    const summaryResponse:
        PortfolioSummaryResponse = {
        portfolioId: portfolio.id,
        portfolioName: portfolio.name,
        initialValue: 10000,
        currentValue: 11250,
        totalRealizedProfit: 350,
        totalUnrealizedProfit: 900,
        totalProfit: 1250,
        totalReturnPercent: 12.5,
        activeAssetCount: 4,
        transactionCount: 12,
        totalBuyAmount: 12500,
        totalSellAmount: 2500,
        totalDividendAmount: 100,
        totalFeeAmount: 25,
        totalDepositAmount: 10000,
        totalWithdrawalAmount: 0,
        netCashFlow: 10000,
        calculatedAt: '2026-07-29T20:00:00Z',
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                PortfolioService,
                provideHttpClient(),
                provideHttpClientTesting(),
            ],
        });

        service = TestBed.inject(PortfolioService);

        httpTestingController = TestBed.inject(
            HttpTestingController,
        );
    });

    afterEach(() => {
        httpTestingController.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should get portfolios without optional query parameters', () => {
        let actualResponse:
            PagedResponse<PortfolioResponse> | undefined;

        service.getPortfolios().subscribe((response) => {
            actualResponse = response;
        });

        const request =
            httpTestingController.expectOne(
                portfoliosUrl,
            );

        expect(request.request.method).toBe('GET');
        expect(request.request.params.keys()).toEqual([]);

        request.flush(pagedResponse);

        expect(actualResponse).toEqual(pagedResponse);
    });

    it('should get portfolios with pagination and sorting parameters', () => {
        service
            .getPortfolios({
                page: 2,
                size: 10,
                sortBy: 'name',
                sortDirection: 'asc',
            })
            .subscribe();

        const request =
            httpTestingController.expectOne(
                (candidate) =>
                    candidate.url === portfoliosUrl,
            );

        expect(request.request.method).toBe('GET');
        expect(request.request.params.get('page')).toBe(
            '2',
        );
        expect(request.request.params.get('size')).toBe(
            '10',
        );
        expect(
            request.request.params.get('sortBy'),
        ).toBe('name');
        expect(
            request.request.params.get(
                'sortDirection',
            ),
        ).toBe('asc');

        request.flush(pagedResponse);
    });

    it('should get a portfolio by id', () => {
        let actualResponse:
            PortfolioResponse | undefined;

        service
            .getPortfolio(portfolio.id)
            .subscribe((response) => {
                actualResponse = response;
            });

        const request =
            httpTestingController.expectOne(
                `${portfoliosUrl}/${portfolio.id}`,
            );

        expect(request.request.method).toBe('GET');

        request.flush(portfolio);

        expect(actualResponse).toEqual(portfolio);
    });

    it('should create a portfolio', () => {
        const createRequest:
            PortfolioCreateRequest = {
            name: 'Growth Portfolio',
            creationMethod: 'BY_AMOUNT',
            initialValue: 10000,
        };

        let actualResponse:
            PortfolioResponse | undefined;

        service
            .createPortfolio(createRequest)
            .subscribe((response) => {
                actualResponse = response;
            });

        const request =
            httpTestingController.expectOne(
                portfoliosUrl,
            );

        expect(request.request.method).toBe('POST');
        expect(request.request.body).toEqual(
            createRequest,
        );

        request.flush(portfolio);

        expect(actualResponse).toEqual(portfolio);
    });

    it('should rename a portfolio', () => {
        const renameRequest:
            PortfolioRenameRequest = {
            name: 'Updated Portfolio',
        };

        service
            .renamePortfolio(
                portfolio.id,
                renameRequest,
            )
            .subscribe();

        const request =
            httpTestingController.expectOne(
                `${portfoliosUrl}/${portfolio.id}/name`,
            );

        expect(request.request.method).toBe('PATCH');
        expect(request.request.body).toEqual(
            renameRequest,
        );

        request.flush({
            ...portfolio,
            name: renameRequest.name,
        });
    });

    it('should update a portfolio value', () => {
        const valueRequest:
            PortfolioValueUpdateRequest = {
            currentValue: 12000,
        };

        service
            .updatePortfolioValue(
                portfolio.id,
                valueRequest,
            )
            .subscribe();

        const request =
            httpTestingController.expectOne(
                `${portfoliosUrl}/${portfolio.id}/value`,
            );

        expect(request.request.method).toBe('PATCH');
        expect(request.request.body).toEqual(
            valueRequest,
        );

        request.flush({
            ...portfolio,
            currentValue: valueRequest.currentValue,
        });
    });

    it('should delete a portfolio', () => {
        let completed = false;

        service
            .deletePortfolio(portfolio.id)
            .subscribe({
                complete: () => {
                    completed = true;
                },
            });

        const request =
            httpTestingController.expectOne(
                `${portfoliosUrl}/${portfolio.id}`,
            );

        expect(request.request.method).toBe('DELETE');

        request.flush(null);

        expect(completed).toBe(true);
    });

    it('should get a portfolio summary', () => {
        let actualResponse:
            PortfolioSummaryResponse | undefined;

        service
            .getPortfolioSummary(portfolio.id)
            .subscribe((response) => {
                actualResponse = response;
            });

        const request =
            httpTestingController.expectOne(
                `${portfoliosUrl}/${portfolio.id}/analytics/summary`,
            );

        expect(request.request.method).toBe('GET');

        request.flush(summaryResponse);

        expect(actualResponse).toEqual(
            summaryResponse,
        );
    });
});
