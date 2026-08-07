import {
    Component,
    computed,
    inject,
    signal,
} from '@angular/core';
import {
    FormBuilder,
    ReactiveFormsModule,
    Validators,
} from '@angular/forms';
import {
    HttpErrorResponse,
} from '@angular/common/http';
import {
    ActivatedRoute,
    Router,
    RouterLink,
} from '@angular/router';
import {
    catchError,
    finalize,
    of,
    switchMap,
} from 'rxjs';

import {
    ApiErrorResponse,
    LoginRequest,
} from '../../../../core/auth/models/auth.models';
import {
    AuthService,
} from '../../../../core/auth/services/auth.service';
import {
    TranslationService,
} from '../../../../core/i18n/services/translation.service';
import {
    PortfolioService,
} from '../../../../core/portfolio/services/portfolio.service';

@Component({
    selector: 'app-login-page',
    imports: [
        ReactiveFormsModule,
        RouterLink,
    ],
    templateUrl: './login-page.html',
    styleUrl: './login-page.scss',
})
export class LoginPage {
    private readonly formBuilder =
        inject(FormBuilder);

    private readonly authService =
        inject(AuthService);

    private readonly portfolioService =
        inject(PortfolioService);

    private readonly router =
        inject(Router);

    private readonly activatedRoute =
        inject(ActivatedRoute);

    private readonly translationService =
        inject(TranslationService);

    protected readonly text =
        this.translationService.text;

    protected readonly isSubmitting =
        signal(false);

    protected readonly errorMessage =
        signal<string | null>(
            null,
        );

    protected readonly registrationMessage =
        signal<string | null>(
            this.resolveRegistrationMessage(),
        );

    protected readonly loginForm =
        this.formBuilder.nonNullable.group({
            email: [
                '',
                [
                    Validators.required,
                    Validators.email,
                    Validators.maxLength(320),
                ],
            ],
            password: [
                '',
                [
                    Validators.required,
                    Validators.minLength(8),
                    Validators.maxLength(100),
                ],
            ],
        });

    protected readonly emailControl =
        this.loginForm.controls.email;

    protected readonly passwordControl =
        this.loginForm.controls.password;

    protected readonly submitButtonLabel =
        computed(() =>
            this.isSubmitting()
                ? this.text().auth.login.signingIn
                : this.text().auth.login.login,
        );

    protected submit(): void {
        this.errorMessage.set(null);

        this.normalizeEmail();

        if (this.loginForm.invalid) {
            this.loginForm.markAllAsTouched();
            return;
        }

        const request: LoginRequest =
            this.loginForm.getRawValue();

        this.isSubmitting.set(true);

        this.authService
            .login(request)
            .pipe(
                switchMap(() =>
                    this.authService.loadCurrentUser(),
                ),
                switchMap(() =>
                    this.portfolioService
                        .refreshMarketData()
                        .pipe(
                            catchError(() =>
                                of(null),
                            ),
                        ),
                ),
                finalize(() => {
                    this.isSubmitting.set(false);
                }),
            )
            .subscribe({
                next: () => {
                    void this.router.navigateByUrl(
                        this.resolvePostLoginUrl(),
                    );
                },
                error: (error: unknown) => {
                    this.errorMessage.set(
                        this.resolveErrorMessage(error),
                    );
                },
            });
    }

    protected shouldShowEmailRequiredError(): boolean {
        return (
            this.emailControl.touched &&
            this.emailControl.hasError('required')
        );
    }

    protected shouldShowEmailFormatError(): boolean {
        return (
            this.emailControl.touched &&
            this.emailControl.hasError('email')
        );
    }

    protected shouldShowPasswordRequiredError(): boolean {
        return (
            this.passwordControl.touched &&
            this.passwordControl.hasError('required')
        );
    }

    protected shouldShowPasswordLengthError(): boolean {
        return (
            this.passwordControl.touched &&
            (
                this.passwordControl.hasError('minlength') ||
                this.passwordControl.hasError('maxlength')
            )
        );
    }

    private normalizeEmail(): void {
        const normalizedEmail =
            this.emailControl.value
                .trim()
                .toLowerCase();

        this.emailControl.setValue(
            normalizedEmail,
            {
                emitEvent: false,
            },
        );

        this.emailControl.updateValueAndValidity({
            emitEvent: false,
        });
    }

    private resolveRegistrationMessage(): string | null {
        const registrationCompleted =
            this.activatedRoute.snapshot.queryParamMap.get(
                'registered',
            );

        if (registrationCompleted !== 'true') {
            return null;
        }

        return this.text().auth.login.registrationSuccess;
    }

    private resolvePostLoginUrl(): string {
        const returnUrl =
            this.activatedRoute.snapshot.queryParamMap.get(
                'returnUrl',
            );

        if (!this.isSafeInternalUrl(returnUrl)) {
            return '/dashboard';
        }

        return returnUrl;
    }

    private isSafeInternalUrl(
        url: string | null,
    ): url is string {
        if (!url) {
            return false;
        }

        const normalizedUrl = url.trim();

        if (
            !normalizedUrl.startsWith('/') ||
            normalizedUrl.startsWith('//') ||
            normalizedUrl.includes('\\')
        ) {
            return false;
        }

        try {
            const parsedUrl =
                this.router.parseUrl(normalizedUrl);

            return (
                parsedUrl.root.children['primary'] !==
                undefined
            );
        } catch {
            return false;
        }
    }

    private resolveErrorMessage(
        error: unknown,
    ): string {
        const translations =
            this.text().auth.login;

        if (!(error instanceof HttpErrorResponse)) {
            return translations.genericError;
        }

        if (error.status === 0) {
            return translations.serverUnavailable;
        }

        if (
            error.status === 400 ||
            error.status === 401
        ) {
            return translations.invalidCredentials;
        }

        if (error.status === 403) {
            return translations.forbidden;
        }

        const response =
            error.error as ApiErrorResponse | null;

        if (
            response &&
            typeof response.message === 'string' &&
            response.message.trim().length > 0
        ) {
            return response.message;
        }

        return translations.loginFailed;
    }
}