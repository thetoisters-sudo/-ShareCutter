import { provideHttpClient } from '@angular/common/http';
import {
    HttpTestingController,
    provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import {
    AssetCreateRequest,
    AssetResponse,
    AssetUpdateRequest,
} from '../models/asset.models';
import { AssetService } from './asset.service';

describe('AssetService', () => {
    let service: AssetService;
    let httpTestingController:
        HttpTestingController;

    const portfolioId = 'portfolio-1';
    const assetId = 'asset-1';

    const assetsUrl =
        `${environment.apiBaseUrl}` +
        `/portfolios/${portfolioId}/assets`;

    const asset: AssetResponse = {
        id: assetId,
        portfolioId,
        symbol: 'AAPL',
        displayName: 'Apple Inc.',
        assetType: 'STOCK',
        currency: 'USD',
        isin: 'US0378331005',
        exchange: 'NASDAQ',
        notes: 'Core technology holding',
        createdAt: '2026-07-30T10:00:00Z',
        updatedAt: '2026-07-30T10:00:00Z',
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                AssetService,
                provideHttpClient(),
                provideHttpClientTesting(),
            ],
        });

        service = TestBed.inject(AssetService);

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

    it('should get all assets for a portfolio', () => {
        let actualResponse:
            AssetResponse[] | undefined;

        service
            .getAssets(portfolioId)
            .subscribe((response) => {
                actualResponse = response;
            });

        const request =
            httpTestingController.expectOne(
                assetsUrl,
            );

        expect(request.request.method).toBe('GET');

        request.flush([asset]);

        expect(actualResponse).toEqual([asset]);
    });

    it('should get an asset by id', () => {
        let actualResponse:
            AssetResponse | undefined;

        service
            .getAsset(
                portfolioId,
                assetId,
            )
            .subscribe((response) => {
                actualResponse = response;
            });

        const request =
            httpTestingController.expectOne(
                `${assetsUrl}/${assetId}`,
            );

        expect(request.request.method).toBe('GET');

        request.flush(asset);

        expect(actualResponse).toEqual(asset);
    });

    it('should create an asset', () => {
        const createRequest:
            AssetCreateRequest = {
            symbol: 'AAPL',
            displayName: 'Apple Inc.',
            assetType: 'STOCK',
            currency: 'USD',
            isin: 'US0378331005',
            exchange: 'NASDAQ',
            notes: 'Core technology holding',
        };

        let actualResponse:
            AssetResponse | undefined;

        service
            .createAsset(
                portfolioId,
                createRequest,
            )
            .subscribe((response) => {
                actualResponse = response;
            });

        const request =
            httpTestingController.expectOne(
                assetsUrl,
            );

        expect(request.request.method).toBe('POST');

        expect(request.request.body).toEqual(
            createRequest,
        );

        request.flush(asset);

        expect(actualResponse).toEqual(asset);
    });

    it('should create an asset with nullable optional fields', () => {
        const createRequest:
            AssetCreateRequest = {
            symbol: 'CASH',
            displayName: 'US Dollar Cash',
            assetType: 'CASH',
            currency: 'USD',
            isin: null,
            exchange: null,
            notes: null,
        };

        service
            .createAsset(
                portfolioId,
                createRequest,
            )
            .subscribe();

        const request =
            httpTestingController.expectOne(
                assetsUrl,
            );

        expect(request.request.method).toBe('POST');

        expect(request.request.body).toEqual(
            createRequest,
        );

        request.flush({
            ...asset,
            ...createRequest,
        });
    });

    it('should update an asset', () => {
        const updateRequest:
            AssetUpdateRequest = {
            symbol: 'MSFT',
            displayName: 'Microsoft Corporation',
            assetType: 'STOCK',
            currency: 'USD',
            isin: 'US5949181045',
            exchange: 'NASDAQ',
            notes: 'Updated technology holding',
        };

        let actualResponse:
            AssetResponse | undefined;

        service
            .updateAsset(
                portfolioId,
                assetId,
                updateRequest,
            )
            .subscribe((response) => {
                actualResponse = response;
            });

        const request =
            httpTestingController.expectOne(
                `${assetsUrl}/${assetId}`,
            );

        expect(request.request.method).toBe('PUT');

        expect(request.request.body).toEqual(
            updateRequest,
        );

        const updatedAsset: AssetResponse = {
            ...asset,
            ...updateRequest,
            updatedAt: '2026-07-30T11:00:00Z',
        };

        request.flush(updatedAsset);

        expect(actualResponse).toEqual(
            updatedAsset,
        );
    });

    it('should delete an asset', () => {
        let completed = false;

        service
            .deleteAsset(
                portfolioId,
                assetId,
            )
            .subscribe({
                complete: () => {
                    completed = true;
                },
            });

        const request =
            httpTestingController.expectOne(
                `${assetsUrl}/${assetId}`,
            );

        expect(request.request.method).toBe(
            'DELETE',
        );

        request.flush(null);

        expect(completed).toBe(true);
    });
});