import { TestBed } from '@angular/core/testing';

import { TokenResponse } from '../models/auth.models';
import { TokenStorageService } from './token-storage.service';

describe('TokenStorageService', () => {
    let service: TokenStorageService;

    const tokenResponse: TokenResponse = {
        accessToken: 'access-token-value',
        refreshToken: 'refresh-token-value',
        tokenType: 'Bearer',
        expiresIn: 900,
    };

    beforeEach(() => {
        TestBed.configureTestingModule({});

        service = TestBed.inject(TokenStorageService);
        localStorage.clear();
    });

    afterEach(() => {
        localStorage.clear();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should save and return authentication tokens', () => {
        service.saveTokens(tokenResponse);

        expect(service.getAccessToken()).toBe(
            tokenResponse.accessToken,
        );

        expect(service.getRefreshToken()).toBe(
            tokenResponse.refreshToken,
        );

        expect(service.getTokenType()).toBe(
            tokenResponse.tokenType,
        );

        expect(service.getAuthorizationHeader()).toBe(
            'Bearer access-token-value',
        );
    });

    it('should report that an access token exists', () => {
        expect(service.hasAccessToken()).toBe(false);

        service.saveTokens(tokenResponse);

        expect(service.hasAccessToken()).toBe(true);
    });

    it('should report a newly saved access token as valid', () => {
        service.saveTokens(tokenResponse);

        expect(service.isAccessTokenExpired()).toBe(false);
    });

    it('should report a missing expiration value as expired', () => {
        expect(service.isAccessTokenExpired()).toBe(true);
    });

    it('should return Bearer as the default token type', () => {
        expect(service.getTokenType()).toBe('Bearer');
    });

    it('should return null when no access token exists', () => {
        expect(service.getAuthorizationHeader()).toBeNull();
    });

    it('should remove all stored authentication data', () => {
        service.saveTokens(tokenResponse);

        service.clear();

        expect(service.getAccessToken()).toBeNull();
        expect(service.getRefreshToken()).toBeNull();
        expect(service.hasAccessToken()).toBe(false);
        expect(service.getAuthorizationHeader()).toBeNull();
        expect(service.isAccessTokenExpired()).toBe(true);
    });
});