import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
    ActivatedRouteSnapshot,
    Router,
    RouterStateSnapshot,
    UrlTree,
    provideRouter,
} from '@angular/router';

import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
    const authenticatedState = signal(false);

    const authServiceMock = {
        isAuthenticated:
            authenticatedState.asReadonly(),
    };

    let router: Router;

    beforeEach(() => {
        authenticatedState.set(false);

        TestBed.configureTestingModule({
            providers: [
                provideRouter([]),
                {
                    provide: AuthService,
                    useValue: authServiceMock,
                },
            ],
        });

        router = TestBed.inject(Router);
    });

    it(
        'should allow navigation for an authenticated user',
        () => {
            authenticatedState.set(true);

            const result = executeGuard(
                '/portfolios',
            );

            expect(result).toBe(true);
        },
    );

    it(
        'should redirect an unauthenticated user to login',
        () => {
            const result = executeGuard(
                '/dashboard',
            );

            expect(result).toBeInstanceOf(UrlTree);

            const redirectedUrl =
                router.serializeUrl(
                    result as UrlTree,
                );

            expect(redirectedUrl).toBe(
                '/login?returnUrl=%2Fdashboard',
            );
        },
    );

    it(
        'should preserve the requested URL in returnUrl',
        () => {
            const result = executeGuard(
                '/portfolios?view=active',
            );

            expect(result).toBeInstanceOf(UrlTree);

            const redirectedUrl =
                router.serializeUrl(
                    result as UrlTree,
                );

            expect(redirectedUrl).toBe(
                '/login?returnUrl=%2Fportfolios%3Fview%3Dactive',
            );
        },
    );
});

function executeGuard(
    requestedUrl: string,
): boolean | UrlTree {
    return TestBed.runInInjectionContext(
        () =>
            authGuard(
                {} as ActivatedRouteSnapshot,
                {
                    url: requestedUrl,
                } as RouterStateSnapshot,
            ) as boolean | UrlTree,
    );
}