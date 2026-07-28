import { Injectable } from '@angular/core';

import { TokenResponse } from '../models/auth.models';

@Injectable({
    providedIn: 'root',
})
export class TokenStorageService {
    private readonly accessTokenKey =
        'sharecutter.accessToken';

    private readonly refreshTokenKey =
        'sharecutter.refreshToken';

    private readonly tokenTypeKey =
        'sharecutter.tokenType';

    private readonly tokenExpiresAtKey =
        'sharecutter.tokenExpiresAt';

    saveTokens(tokenResponse: TokenResponse): void {
        const expiresAt =
            Date.now() + tokenResponse.expiresIn * 1000;

        localStorage.setItem(
            this.accessTokenKey,
            tokenResponse.accessToken,
        );

        localStorage.setItem(
            this.refreshTokenKey,
            tokenResponse.refreshToken,
        );

        localStorage.setItem(
            this.tokenTypeKey,
            tokenResponse.tokenType,
        );

        localStorage.setItem(
            this.tokenExpiresAtKey,
            expiresAt.toString(),
        );
    }

    getAccessToken(): string | null {
        return localStorage.getItem(this.accessTokenKey);
    }

    getRefreshToken(): string | null {
        return localStorage.getItem(this.refreshTokenKey);
    }

    getTokenType(): string {
        return (
            localStorage.getItem(this.tokenTypeKey) ??
            'Bearer'
        );
    }

    getAuthorizationHeader(): string | null {
        const accessToken = this.getAccessToken();

        if (!accessToken) {
            return null;
        }

        return `${this.getTokenType()} ${accessToken}`;
    }

    hasAccessToken(): boolean {
        return this.getAccessToken() !== null;
    }

    isAccessTokenExpired(): boolean {
        const expiresAtValue = localStorage.getItem(
            this.tokenExpiresAtKey,
        );

        if (!expiresAtValue) {
            return true;
        }

        const expiresAt = Number(expiresAtValue);

        if (!Number.isFinite(expiresAt)) {
            return true;
        }

        return Date.now() >= expiresAt;
    }

    clear(): void {
        localStorage.removeItem(this.accessTokenKey);
        localStorage.removeItem(this.refreshTokenKey);
        localStorage.removeItem(this.tokenTypeKey);
        localStorage.removeItem(this.tokenExpiresAtKey);
    }
}