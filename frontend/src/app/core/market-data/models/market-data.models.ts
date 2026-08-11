import {
    AssetType,
} from '../../asset/models/asset.models';

export interface MarketSymbolSearchResponse {
    symbol: string;
    displayName: string;
    exchange: string | null;
    micCode: string | null;
    instrumentType: string | null;
    country: string | null;
    currency: string;
    assetType: AssetType;
}

export interface MarketPriceResponse {
    symbol: string;
    exchange: string | null;
    price: number;
    retrievedAt: string;
}

export interface MarketSymbolSearchRequest {
    query: string;
    limit?: number;
}

export interface MarketPriceRequest {
    symbol: string;
    exchange?: string | null;
}