export type PortfolioCreationMethod =
    | 'BY_AMOUNT'
    | 'BY_HOLDINGS';

export type PortfolioSortDirection =
    | 'asc'
    | 'desc';

export interface PortfolioCreateRequest {
    name: string;
    creationMethod: PortfolioCreationMethod;
    initialValue: number;
}

export interface PortfolioRenameRequest {
    name: string;
}

export interface PortfolioValueUpdateRequest {
    currentValue: number;
}

export interface PortfolioResponse {
    id: string;
    userId: string;
    name: string;
    creationMethod: PortfolioCreationMethod;
    initialValue: number;
    currentValue: number;
    totalRealizedProfit: number;
    totalUnrealizedProfit: number;
    totalReturnPercent: number;
    createdAt: string;
    updatedAt: string;
}

export interface PortfolioListQuery {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDirection?: PortfolioSortDirection;
}

export interface PagedResponse<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
    hasNext: boolean;
    hasPrevious: boolean;
}

export interface PortfolioSummaryResponse {
    portfolioId: string;
    portfolioName: string;
    initialValue: number;
    currentValue: number;
    totalRealizedProfit: number;
    totalUnrealizedProfit: number;
    totalProfit: number;
    totalReturnPercent: number;
    activeAssetCount: number;
    transactionCount: number;
    totalBuyAmount: number;
    totalSellAmount: number;
    totalDividendAmount: number;
    totalFeeAmount: number;
    totalDepositAmount: number;
    totalWithdrawalAmount: number;
    netCashFlow: number;
    calculatedAt: string;
}

export interface AssetAllocationItemResponse {
    assetId: string;
    symbol: string;
    displayName: string;
    assetType: string;
    currency: string;
    quantity: number;
    averageCost: number;
    currentPrice: number;
    totalCost: number;
    marketValue: number;
    realizedProfit: number;
    unrealizedProfit: number;
    allocationPercent: number;
}

export interface PortfolioAllocationResponse {
    portfolioId: string;
    portfolioName: string;

    portfolioValue?: number;
    cashBalance?: number;

    totalMarketValue: number;
    totalCost: number;
    totalRealizedProfit: number;
    totalUnrealizedProfit: number;

    totalTargetWeightPercent?: number;

    allocatedAssetCount: number;
    assets: AssetAllocationItemResponse[];
    calculatedAt: string;
}