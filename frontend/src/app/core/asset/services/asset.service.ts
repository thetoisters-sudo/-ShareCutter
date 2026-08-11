import {
    HttpClient,
} from '@angular/common/http';
import {
    Injectable,
    inject,
} from '@angular/core';
import {
    Observable,
    tap,
} from 'rxjs';

import {
    environment,
} from '../../../../environments/environment';
import {
    PortfolioService,
} from '../../portfolio/services/portfolio.service';
import {
    AssetCreateRequest,
    AssetResponse,
    AssetUpdateRequest,
} from '../models/asset.models';

@Injectable({
    providedIn: 'root',
})
export class AssetService {
    private readonly http =
        inject(HttpClient);

    private readonly portfolioService =
        inject(PortfolioService);

    getAssets(
        portfolioId: string,
    ): Observable<AssetResponse[]> {
        return this.http.get<AssetResponse[]>(
            this.buildAssetsUrl(portfolioId),
        );
    }

    getAsset(
        portfolioId: string,
        assetId: string,
    ): Observable<AssetResponse> {
        return this.http.get<AssetResponse>(
            `${this.buildAssetsUrl(portfolioId)}/${assetId}`,
        );
    }

    createAsset(
        portfolioId: string,
        request: AssetCreateRequest,
    ): Observable<AssetResponse> {
        return this.http.post<AssetResponse>(
            this.buildAssetsUrl(portfolioId),
            request,
        ).pipe(
            tap(() => {
                this.portfolioService
                    .notifyPortfolioChanged({
                        portfolioId,
                        reason: 'asset-created',
                    });
            }),
        );
    }

    updateAsset(
        portfolioId: string,
        assetId: string,
        request: AssetUpdateRequest,
    ): Observable<AssetResponse> {
        return this.http.put<AssetResponse>(
            `${this.buildAssetsUrl(portfolioId)}/${assetId}`,
            request,
        ).pipe(
            tap(() => {
                this.portfolioService
                    .notifyPortfolioChanged({
                        portfolioId,
                        reason: 'asset-updated',
                    });
            }),
        );
    }

    deleteAsset(
        portfolioId: string,
        assetId: string,
    ): Observable<void> {
        return this.http.delete<void>(
            `${this.buildAssetsUrl(portfolioId)}/${assetId}`,
        ).pipe(
            tap(() => {
                this.portfolioService
                    .notifyPortfolioChanged({
                        portfolioId,
                        reason: 'asset-deleted',
                    });
            }),
        );
    }

    private buildAssetsUrl(
        portfolioId: string,
    ): string {
        return (
            `${environment.apiBaseUrl}` +
            `/portfolios/${portfolioId}/assets`
        );
    }
}