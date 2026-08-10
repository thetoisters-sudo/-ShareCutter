export type AllocationPurchaseSuggestedAction =
    | 'BUY'
    | 'SELL'
    | 'HOLD';

export interface AllocationPurchasePreviewRequest {
    assetId: string;
    targetWeightPercent: number;
}

export interface AllocationPurchaseExecuteRequest {
    assetId: string;
    targetWeightPercent: number;
    fee: number | null;
}

export interface AllocationPurchasePreviewResponse {
    portfolioId: string;
    portfolioName: string;
    assetId: string;
    symbol: string;
    displayName: string;
    exchange: string | null;
    currency: string;
    portfolioCurrentValue: number;
    currentPrice: number;
    targetWeightPercent: number;
    weightAssignedToOtherAssetsPercent: number;
    totalTargetWeightPercent: number;
    allocationDifferencePercent: number;
    currentMarketValue: number;
    targetMarketValue: number;
    existingQuantity: number;
    targetQuantity: number;
    quantityDifference: number;
    quantityToBuy: number;
    quantityToSell: number;
    estimatedTradeAmount: number;
    suggestedAction:
    AllocationPurchaseSuggestedAction;
    calculatedAt: string;
}

export interface AllocationPurchaseExecutionResponse {
    portfolioId: string;
    portfolioName: string;
    assetId: string;
    symbol: string;
    displayName: string;
    transactionId: string;
    action:
    AllocationPurchaseSuggestedAction;
    targetWeightPercent: number;
    unitPrice: number;
    tradedQuantity: number;
    tradeAmount: number;
    fee: number;
    cashImpact: number;
    availableCashBefore: number;
    availableCashAfter: number;
    totalTargetWeightPercent: number;
    allocationDifferencePercent: number;
    currency: string;
    executedAt: string;
}