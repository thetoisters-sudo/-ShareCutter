export type AssetType =
    | 'STOCK'
    | 'ETF'
    | 'BOND'
    | 'FUND'
    | 'CRYPTO'
    | 'COMMODITY'
    | 'FOREX'
    | 'CASH'
    | 'OTHER';

export interface AssetResponse {
    id: string;
    portfolioId: string;
    symbol: string;
    displayName: string;
    assetType: AssetType;
    currency: string;
    isin: string | null;
    exchange: string | null;
    notes: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface AssetCreateRequest {
    symbol: string;
    displayName: string;
    assetType: AssetType;
    currency: string;
    isin: string | null;
    exchange: string | null;
    notes: string | null;
}

export interface AssetUpdateRequest {
    symbol: string;
    displayName: string;
    assetType: AssetType;
    currency: string;
    isin: string | null;
    exchange: string | null;
    notes: string | null;
}

export interface AssetTypeOption {
    value: AssetType;
    label: string;
}

export const ASSET_TYPE_OPTIONS:
    readonly AssetTypeOption[] = [
        {
            value: 'STOCK',
            label: 'Stock',
        },
        {
            value: 'ETF',
            label: 'ETF',
        },
        {
            value: 'BOND',
            label: 'Bond',
        },
        {
            value: 'FUND',
            label: 'Fund',
        },
        {
            value: 'CRYPTO',
            label: 'Cryptocurrency',
        },
        {
            value: 'COMMODITY',
            label: 'Commodity',
        },
        {
            value: 'FOREX',
            label: 'Foreign exchange',
        },
        {
            value: 'CASH',
            label: 'Cash',
        },
        {
            value: 'OTHER',
            label: 'Other',
        },
    ];