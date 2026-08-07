export type AllocationPurchaseSuggestedAction =
    | 'BUY'
    | 'SELL_REQUIRED'
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
    portfolioInitialValue: number;
    currentPrice: number;
    targetWeightPercent: number;
    currentlyAssignedWeightPercent: number;
    remainingAssignableWeightPercent: number;
    targetMarketValue: number;
    existingQuantity: number;
    targetQuantity: number;
    quantityToBuy: number;
    estimatedPurchaseAmount: number;
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
    targetWeightPercent: number;
    unitPrice: number;
    purchasedQuantity: number;
    purchaseAmount: number;
    fee: number;
    totalCashUsed: number;
    availableCashBefore: number;
    availableCashAfter: number;
    remainingAssignableWeightPercent: number;
    currency: string;
    executedAt: string;
}