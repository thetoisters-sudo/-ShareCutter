import {
    Injectable,
    computed,
    inject,
    signal,
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
    LoginRequest,
    RegisterRequest,
    TokenResponse,
    UserResponse,
} from '../models/auth.models';
import { TokenStorageService } from './token-storage.service';

@Injectable({
    providedIn: 'root',
})
export class AuthService {
    private readonly http = inject(HttpClient);
    private readonly tokenStorage =
        inject(TokenStorageService);

    private readonly currentUserState =
        signal<UserResponse | null>(null);

    private readonly loadingCurrentUserState =
        signal(false);

    readonly currentUser =
        this.currentUserState.asReadonly();

    readonly isLoadingCurrentUser =
        this.loadingCurrentUserState.asReadonly();

    readonly isAuthenticated = computed(
        () =>
            this.currentUserState() !== null ||
            this.hasValidStoredSession(),
    );

    login(
        request: LoginRequest,
    ): Observable<TokenResponse> {
        return this.http
            .post<TokenResponse>(
                `${environment.apiBaseUrl}/auth/login`,
                request,
            )
            .pipe(
                tap((response) => {
                    this.tokenStorage.saveTokens(response);
                }),
            );
    }

    register(
        request: RegisterRequest,
    ): Observable<UserResponse> {
        return this.http.post<UserResponse>(
            `${environment.apiBaseUrl}/users`,
            request,
        );
    }

    loadCurrentUser(): Observable<UserResponse> {
        this.loadingCurrentUserState.set(true);

        return this.http
            .get<UserResponse>(
                `${environment.apiBaseUrl}/users/me`,
            )
            .pipe(
                tap({
                    next: (user) => {
                        this.currentUserState.set(user);
                        this.loadingCurrentUserState.set(false);
                    },
                    error: () => {
                        this.currentUserState.set(null);
                        this.loadingCurrentUserState.set(false);
                    },
                }),
            );
    }

    setCurrentUser(user: UserResponse): void {
        this.currentUserState.set(user);
    }

    logout(): void {
        this.tokenStorage.clear();
        this.currentUserState.set(null);
    }

    hasValidStoredSession(): boolean {
        return (
            this.tokenStorage.hasAccessToken() &&
            !this.tokenStorage.isAccessTokenExpired()
        );
    }
}