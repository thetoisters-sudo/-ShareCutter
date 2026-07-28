import { inject } from '@angular/core';
import {
    HttpErrorResponse,
    HttpInterceptorFn,
} from '@angular/common/http';
import { Router } from '@angular/router';
import {
    catchError,
    throwError,
} from 'rxjs';

import { environment } from '../../../../environments/environment';
import { TokenStorageService } from '../services/token-storage.service';

export const authInterceptor: HttpInterceptorFn = (
    request,
    next,
) => {
    const tokenStorage = inject(TokenStorageService);
    const router = inject(Router);

    const isApiRequest = request.url.startsWith(
        environment.apiBaseUrl,
    );

    const isLoginRequest = request.url.endsWith(
        '/auth/login',
    );

    const authorizationHeader =
        tokenStorage.getAuthorizationHeader();

    const authenticatedRequest =
        isApiRequest && authorizationHeader
            ? request.clone({
                setHeaders: {
                    Authorization: authorizationHeader,
                },
            })
            : request;

    return next(authenticatedRequest).pipe(
        catchError((error: unknown) => {
            const isUnauthorized =
                error instanceof HttpErrorResponse &&
                error.status === 401;

            if (isApiRequest && isUnauthorized && !isLoginRequest) {
                tokenStorage.clear();

                void router.navigate(['/login'], {
                    queryParams: {
                        reason: 'session-expired',
                    },
                });
            }

            return throwError(() => error);
        }),
    );
};