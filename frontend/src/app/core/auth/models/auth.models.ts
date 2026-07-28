export type UserRole = 'USER' | 'ADMIN';

export type UserStatus =
    | 'PENDING'
    | 'ACTIVE'
    | 'SUSPENDED';

export interface LoginRequest {
    email: string;
    password: string;
}

export interface RegisterRequest {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
}

export interface RefreshTokenRequest {
    refreshToken: string;
}

export interface TokenResponse {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    expiresIn: number;
}

export interface UserResponse {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    role: UserRole;
    status: UserStatus;
    createdAt: string;
    updatedAt: string;
}

export interface ApiValidationError {
    field: string;
    message: string;
}

export interface ApiErrorResponse {
    timestamp?: string;
    status?: number;
    error?: string;
    message?: string;
    path?: string;
    validationErrors?: ApiValidationError[];
}