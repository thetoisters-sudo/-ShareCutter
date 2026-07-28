import { TestBed } from '@angular/core/testing';
import {
    HttpTestingController,
    provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { environment } from '../../../../environments/environment';
import {
    LoginRequest,
    RegisterRequest,
    TokenResponse,
    UserResponse,
} from '../models/auth.models';
import { AuthService } from './auth.service';
import { TokenStorageService } from './token-storage.service';

describe('AuthService', () => {
    let service: AuthService;
    let httpTestingController: HttpTestingController;
    let tokenStorage: TokenStorageService;

    const loginRequest: LoginRequest = {
        email: 'user@example.com',
        password: 'password123',
    };

    const registerRequest: RegisterRequest = {
        email: 'user@example.com',
        password: 'password123',
        firstName: 'Share',
        lastName: 'Cutter',
    };

    const tokenResponse: TokenResponse = {
        accessToken: 'access-token-value',
        refreshToken: 'refresh-token-value',
        tokenType: 'Bearer',
        expiresIn: 900,
    };

    const userResponse: UserResponse = {
        id: 'b1488ab2-5296-4d11-832d-f84ab834f907',
        email: 'user@example.com',
        firstName: 'Share',
        lastName: 'Cutter',
        role: 'USER',
        status: 'ACTIVE',
        createdAt: '2026-07-28T10:00:00Z',
        updatedAt: '2026-07-28T10:00:00Z',
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
            ],
        });

        service = TestBed.inject(AuthService);

        httpTestingController = TestBed.inject(
            HttpTestingController,
        );

        tokenStorage = TestBed.inject(
            TokenStorageService,
        );

        localStorage.clear();
    });

    afterEach(() => {
        httpTestingController.verify();
        localStorage.clear();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should send a login request and save tokens', () => {
        service.login(loginRequest).subscribe((response) => {
            expect(response).toEqual(tokenResponse);
            expect(tokenStorage.getAccessToken()).toBe(
                tokenResponse.accessToken,
            );
        });

        const request = httpTestingController.expectOne(
            `${environment.apiBaseUrl}/auth/login`,
        );

        expect(request.request.method).toBe('POST');
        expect(request.request.body).toEqual(loginRequest);

        request.flush(tokenResponse);
    });

    it('should send a registration request', () => {
        service
            .register(registerRequest)
            .subscribe((response) => {
                expect(response).toEqual(userResponse);
            });

        const request = httpTestingController.expectOne(
            `${environment.apiBaseUrl}/users`,
        );

        expect(request.request.method).toBe('POST');
        expect(request.request.body).toEqual(registerRequest);

        request.flush(userResponse);
    });

    it('should load and store the current user', () => {
        service.loadCurrentUser().subscribe((response) => {
            expect(response).toEqual(userResponse);
            expect(service.currentUser()).toEqual(userResponse);
            expect(service.isLoadingCurrentUser()).toBe(false);
        });

        expect(service.isLoadingCurrentUser()).toBe(true);

        const request = httpTestingController.expectOne(
            `${environment.apiBaseUrl}/users/me`,
        );

        expect(request.request.method).toBe('GET');

        request.flush(userResponse);
    });

    it('should clear current user state after a failed load', () => {
        service.setCurrentUser(userResponse);

        service.loadCurrentUser().subscribe({
            error: () => {
                expect(service.currentUser()).toBeNull();
                expect(service.isLoadingCurrentUser()).toBe(false);
            },
        });

        const request = httpTestingController.expectOne(
            `${environment.apiBaseUrl}/users/me`,
        );

        request.flush(
            {
                message: 'Unauthorized',
            },
            {
                status: 401,
                statusText: 'Unauthorized',
            },
        );
    });

    it('should recognize a valid stored session', () => {
        tokenStorage.saveTokens(tokenResponse);

        expect(service.hasValidStoredSession()).toBe(true);
        expect(service.isAuthenticated()).toBe(true);
    });

    it('should set the current user explicitly', () => {
        service.setCurrentUser(userResponse);

        expect(service.currentUser()).toEqual(userResponse);
        expect(service.isAuthenticated()).toBe(true);
    });

    it('should clear tokens and current user on logout', () => {
        tokenStorage.saveTokens(tokenResponse);
        service.setCurrentUser(userResponse);

        service.logout();

        expect(tokenStorage.getAccessToken()).toBeNull();
        expect(tokenStorage.getRefreshToken()).toBeNull();
        expect(service.currentUser()).toBeNull();
        expect(service.isAuthenticated()).toBe(false);
    });
});