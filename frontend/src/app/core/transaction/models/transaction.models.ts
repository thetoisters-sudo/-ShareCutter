export type TransactionType =
    | 'BUY'
    | 'SELL'
    | 'DIVIDEND'
    | 'FEE'
    | 'DEPOSIT'
    | 'WITHDRAWAL'
    | 'TRANSFER_IN'
    | 'TRANSFER_OUT';

export interface TransactionResponse {
    id: string;
    portfolioId: string;
    assetId: string | null;
    transactionType: TransactionType;
    quantity: number | null;
    unitPrice: number | null;
    fee: number | null;
    totalAmount: number;
    currency: string;
    executedAt: string;
    notes: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface TransactionCreateRequest {
    assetId: string | null;
    transactionType: TransactionType;
    quantity: number | null;
    unitPrice: number | null;
    fee: number | null;
    totalAmount: number;
    currency: string;
    executedAt: string;
    notes: string | null;
}

export interface TransactionUpdateRequest {
    assetId: string | null;
    transactionType: TransactionType;
    quantity: number | null;
    unitPrice: number | null;
    fee: number | null;
    totalAmount: number;
    currency: string;
    executedAt: string;
    notes: string | null;
}

export interface TransactionSearchQuery {
    assetId?: string;
    transactionType?: TransactionType;
    startDate?: string;
    endDate?: string;
    page?: number;
    size?: number;
}

export interface TransactionTypeOption {
    value: TransactionType;
    label: string;
}

export const TRANSACTION_TYPE_OPTIONS:
    readonly TransactionTypeOption[] = [
        {
            value: 'BUY',
            label: 'Buy',
        },
        {
            value: 'SELL',
            label: 'Sell',
        },
        {
            value: 'DIVIDEND',
            label: 'Dividend',
        },
        {
            value: 'FEE',
            label: 'Fee',
        },
        {
            value: 'DEPOSIT',
            label: 'Deposit',
        },
        {
            value: 'WITHDRAWAL',
            label: 'Withdrawal',
        },
        {
            value: 'TRANSFER_IN',
            label: 'Transfer In',
        },
        {
            value: 'TRANSFER_OUT',
            label: 'Transfer Out',
        },
    ];