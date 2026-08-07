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
    environment,
} from '../../../../environments/environment';
import {
    MarketPriceResponse,
    MarketSymbolSearchResponse,
} from '../models/market-data.models';
import {
    MarketDataService,
} from './market-data.service';

describe('MarketDataService', () => {
    let service: MarketDataService;
    let httpTestingController:
        HttpTestingController;

    const marketDataUrl =
        `${environment.apiBaseUrl}/market-data`;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                MarketDataService,
                provideHttpClient(),
                provideHttpClientTesting(),
            ],
        });

        service =
            TestBed.inject(
                MarketDataService,
            );

        httpTestingController =
            TestBed.inject(
                HttpTestingController,
            );
    });

    afterEach(() => {
        httpTestingController.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should search market symbols with the default backend limit', () => {
        const response:
            MarketSymbolSearchResponse[] = [
                {
                    symbol: 'TTWO',
                    displayName:
                        'Take-Two Interactive Software Inc.',
                    exchange: 'NASDAQ',
                    micCode: 'XNAS',
                    instrumentType:
                        'Common Stock',
                    country:
                        'United States',
                    currency: 'USD',
                    assetType: 'STOCK',
                },
            ];

        let actualResponse:
            MarketSymbolSearchResponse[] | undefined;

        service
            .searchSymbols({
                query: 'Take Two',
            })
            .subscribe((result) => {
                actualResponse = result;
            });

        const request =
            httpTestingController.expectOne(
                (candidate) =>
                    candidate.url ===
                    `${marketDataUrl}/search` &&
                    candidate.params.get(
                        'query',
                    ) === 'Take Two' &&
                    !candidate.params.has(
                        'limit',
                    ),
            );

        expect(request.request.method)
            .toBe('GET');

        request.flush(response);

        expect(actualResponse)
            .toEqual(response);
    });

    it('should search market symbols with an explicit limit', () => {
        service
            .searchSymbols({
                query: 'NVDA',
                limit: 5,
            })
            .subscribe();

        const request =
            httpTestingController.expectOne(
                (candidate) =>
                    candidate.url ===
                    `${marketDataUrl}/search` &&
                    candidate.params.get(
                        'query',
                    ) === 'NVDA' &&
                    candidate.params.get(
                        'limit',
                    ) === '5',
            );

        expect(request.request.method)
            .toBe('GET');

        request.flush([]);
    });

    it('should request a market price with an exchange', () => {
        const response:
            MarketPriceResponse = {
            symbol: 'TTWO',
            exchange: 'NASDAQ',
            price: 205.55,
            retrievedAt:
                '2026-08-05T13:00:00Z',
        };

        let actualResponse:
            MarketPriceResponse | undefined;

        service
            .getLatestPrice({
                symbol: 'TTWO',
                exchange: 'NASDAQ',
            })
            .subscribe((result) => {
                actualResponse = result;
            });

        const request =
            httpTestingController.expectOne(
                (candidate) =>
                    candidate.url ===
                    `${marketDataUrl}/price` &&
                    candidate.params.get(
                        'symbol',
                    ) === 'TTWO' &&
                    candidate.params.get(
                        'exchange',
                    ) === 'NASDAQ',
            );

        expect(request.request.method)
            .toBe('GET');

        request.flush(response);

        expect(actualResponse)
            .toEqual(response);
    });

    it('should omit an empty exchange from a price request', () => {
        service
            .getLatestPrice({
                symbol: 'AAPL',
                exchange: '   ',
            })
            .subscribe();

        const request =
            httpTestingController.expectOne(
                (candidate) =>
                    candidate.url ===
                    `${marketDataUrl}/price` &&
                    candidate.params.get(
                        'symbol',
                    ) === 'AAPL' &&
                    !candidate.params.has(
                        'exchange',
                    ),
            );

        expect(request.request.method)
            .toBe('GET');

        request.flush({
            symbol: 'AAPL',
            exchange: null,
            price: 200,
            retrievedAt:
                '2026-08-05T13:00:00Z',
        });
    });
});