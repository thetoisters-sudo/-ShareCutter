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
import { guestGuard } from './guest.guard';

describe('guestGuard', () => {
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
        'should allow navigation for a guest user',
        () => {
            const result = executeGuard('/login');

            expect(result).toBe(true);
        },
    );

    it(
        'should redirect an authenticated user to dashboard',
        () => {
            authenticatedState.set(true);

            const result = executeGuard('/login');

            expect(result).toBeInstanceOf(UrlTree);

            const redirectedUrl =
                router.serializeUrl(
                    result as UrlTree,
                );

            expect(redirectedUrl).toBe(
                '/dashboard',
            );
        },
    );

    it(
        'should also redirect authenticated users away from registration',
        () => {
            authenticatedState.set(true);

            const result = executeGuard(
                '/register',
            );

            expect(result).toBeInstanceOf(UrlTree);

            const redirectedUrl =
                router.serializeUrl(
                    result as UrlTree,
                );

            expect(redirectedUrl).toBe(
                '/dashboard',
            );
        },
    );
});

function executeGuard(
    requestedUrl: string,
): boolean | UrlTree {
    return TestBed.runInInjectionContext(
        () =>
            guestGuard(
                {} as ActivatedRouteSnapshot,
                {
                    url: requestedUrl,
                } as RouterStateSnapshot,
            ) as boolean | UrlTree,
    );
}