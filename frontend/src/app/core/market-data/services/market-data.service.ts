import {
    HttpClient,
    HttpParams,
} from '@angular/common/http';
import {
    Injectable,
    inject,
} from '@angular/core';
import {
    Observable,
} from 'rxjs';

import {
    environment,
} from '../../../../environments/environment';
import {
    MarketPriceRequest,
    MarketPriceResponse,
    MarketSymbolSearchRequest,
    MarketSymbolSearchResponse,
} from '../models/market-data.models';

@Injectable({
    providedIn: 'root',
})
export class MarketDataService {
    private readonly http =
        inject(HttpClient);

    private readonly marketDataUrl =
        `${environment.apiBaseUrl}/market-data`;

    searchSymbols(
        request: MarketSymbolSearchRequest,
    ): Observable<MarketSymbolSearchResponse[]> {
        let params = new HttpParams()
            .set(
                'query',
                request.query,
            );

        if (request.limit !== undefined) {
            params = params.set(
                'limit',
                request.limit.toString(),
            );
        }

        return this.http.get<
            MarketSymbolSearchResponse[]
        >(
            `${this.marketDataUrl}/search`,
            {
                params,
            },
        );
    }

    getLatestPrice(
        request: MarketPriceRequest,
    ): Observable<MarketPriceResponse> {
        let params = new HttpParams()
            .set(
                'symbol',
                request.symbol,
            );

        const exchange =
            request.exchange?.trim();

        if (exchange) {
            params = params.set(
                'exchange',
                exchange,
            );
        }

        return this.http.get<
            MarketPriceResponse
        >(
            `${this.marketDataUrl}/price`,
            {
                params,
            },
        );
    }
}