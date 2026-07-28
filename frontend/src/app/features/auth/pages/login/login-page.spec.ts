import { HttpErrorResponse } from '@angular/common/http';
import {
    ComponentFixture,
    TestBed,
} from '@angular/core/testing';
import { FormGroup } from '@angular/forms';
import {
    ActivatedRoute,
    convertToParamMap,
    provideRouter,
    Router,
} from '@angular/router';
import {
    of,
    throwError,
} from 'rxjs';
import { vi } from 'vitest';

import {
    LoginRequest,
    TokenResponse,
    UserResponse,
} from '../../../../core/auth/models/auth.models';
import { AuthService } from '../../../../core/auth/services/auth.service';
import { LoginPage } from './login-page';

interface LoginPageTestAccess {
    loginForm: FormGroup;
    isSubmitting: () => boolean;
    errorMessage: () => string | null;
    registrationMessage: () => string | null;
    submitButtonLabel: () => string;
    submit: () => void;
}

describe('LoginPage', () => {
    let fixture: ComponentFixture<LoginPage>;
    let component: LoginPage;
    let testAccess: LoginPageTestAccess;
    let router: Router;

    const loginMock = vi.fn();
    const loadCurrentUserMock = vi.fn();

    const activatedRouteStub = {
        snapshot: {
            queryParamMap: convertToParamMap({}),
        },
    };

    const authServiceMock = {
        login: loginMock,
        loadCurrentUser: loadCurrentUserMock,
    };

    const loginRequest: LoginRequest = {
        email: 'user@example.com',
        password: 'password123',
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

    const createComponent = (): void => {
        fixture = TestBed.createComponent(LoginPage);
        component = fixture.componentInstance;

        testAccess =
            component as unknown as LoginPageTestAccess;

        router = TestBed.inject(Router);

        fixture.detectChanges();
    };

    beforeEach(async () => {
        loginMock.mockReset();
        loadCurrentUserMock.mockReset();

        activatedRouteStub.snapshot.queryParamMap =
            convertToParamMap({});

        await TestBed.configureTestingModule({
            imports: [LoginPage],
            providers: [
                provideRouter([]),
                {
                    provide: AuthService,
                    useValue: authServiceMock,
                },
                {
                    provide: ActivatedRoute,
                    useValue: activatedRouteStub,
                },
            ],
        }).compileComponents();

        createComponent();
    });

    it('should create the login page', () => {
        expect(component).toBeTruthy();
    });

    it('should render the login form', () => {
        const element =
            fixture.nativeElement as HTMLElement;

        expect(
            element.querySelector('h1')?.textContent,
        ).toContain('Welcome back');

        expect(
            element.querySelector('input[type="email"]'),
        ).toBeTruthy();

        expect(
            element.querySelector('input[type="password"]'),
        ).toBeTruthy();

        expect(
            element.querySelector(
                'button[type="submit"]',
            ),
        ).toBeTruthy();
    });

    it('should display a successful registration message', () => {
        fixture.destroy();

        activatedRouteStub.snapshot.queryParamMap =
            convertToParamMap({
                registered: 'true',
            });

        createComponent();

        expect(testAccess.registrationMessage()).toBe(
            'Your account was created successfully. ' +
            'You can now log in.',
        );

        const successMessage =
            fixture.nativeElement.querySelector(
                '[role="status"]',
            ) as HTMLElement | null;

        expect(successMessage?.textContent).toContain(
            'Your account was created successfully.',
        );
    });

    it('should not display a registration message by default', () => {
        expect(
            testAccess.registrationMessage(),
        ).toBeNull();

        expect(
            fixture.nativeElement.querySelector(
                '[role="status"]',
            ),
        ).toBeNull();
    });

    it('should mark the form as touched when submitted empty', () => {
        testAccess.submit();

        expect(testAccess.loginForm.invalid).toBe(true);

        expect(
            testAccess.loginForm.controls['email'].touched,
        ).toBe(true);

        expect(
            testAccess.loginForm.controls['password'].touched,
        ).toBe(true);

        expect(loginMock).not.toHaveBeenCalled();
    });

    it('should reject an invalid email address', () => {
        testAccess.loginForm.setValue({
            email: 'invalid-email',
            password: 'password123',
        });

        testAccess.submit();

        expect(
            testAccess.loginForm.controls['email']
                .hasError('email'),
        ).toBe(true);

        expect(loginMock).not.toHaveBeenCalled();
    });

    it('should reject a password shorter than eight characters', () => {
        testAccess.loginForm.setValue({
            email: 'user@example.com',
            password: 'short',
        });

        testAccess.submit();

        expect(
            testAccess.loginForm.controls['password']
                .hasError('minlength'),
        ).toBe(true);

        expect(loginMock).not.toHaveBeenCalled();
    });

    it('should normalize the email before login', () => {
        const navigateSpy = vi
            .spyOn(router, 'navigate')
            .mockResolvedValue(true);

        loginMock.mockReturnValue(
            of(tokenResponse),
        );

        loadCurrentUserMock.mockReturnValue(
            of(userResponse),
        );

        testAccess.loginForm.setValue({
            email: '  USER@EXAMPLE.COM  ',
            password: 'password123',
        });

        testAccess.submit();

        expect(loginMock).toHaveBeenCalledWith({
            email: 'user@example.com',
            password: 'password123',
        });

        expect(navigateSpy).toHaveBeenCalledWith([
            '/dashboard',
        ]);
    });

    it('should log in, load the current user and navigate to dashboard', () => {
        const navigateSpy = vi
            .spyOn(router, 'navigate')
            .mockResolvedValue(true);

        loginMock.mockReturnValue(
            of(tokenResponse),
        );

        loadCurrentUserMock.mockReturnValue(
            of(userResponse),
        );

        testAccess.loginForm.setValue(loginRequest);

        testAccess.submit();

        expect(loginMock).toHaveBeenCalledTimes(1);

        expect(loginMock).toHaveBeenCalledWith(
            loginRequest,
        );

        expect(
            loadCurrentUserMock,
        ).toHaveBeenCalledTimes(1);

        expect(navigateSpy).toHaveBeenCalledWith([
            '/dashboard',
        ]);

        expect(testAccess.isSubmitting()).toBe(false);
        expect(testAccess.errorMessage()).toBeNull();
    });

    it('should display an invalid credentials message for a 401 response', () => {
        loginMock.mockReturnValue(
            throwError(
                () =>
                    new HttpErrorResponse({
                        status: 401,
                        statusText: 'Unauthorized',
                        error: {
                            message: 'Invalid credentials',
                        },
                    }),
            ),
        );

        testAccess.loginForm.setValue(loginRequest);

        testAccess.submit();

        expect(testAccess.errorMessage()).toBe(
            'The email or password is incorrect.',
        );

        expect(
            loadCurrentUserMock,
        ).not.toHaveBeenCalled();

        expect(testAccess.isSubmitting()).toBe(false);
    });

    it('should display a server unavailable message for a network error', () => {
        loginMock.mockReturnValue(
            throwError(
                () =>
                    new HttpErrorResponse({
                        status: 0,
                        statusText: 'Unknown Error',
                    }),
            ),
        );

        testAccess.loginForm.setValue(loginRequest);

        testAccess.submit();

        expect(testAccess.errorMessage()).toBe(
            'The ShareCutter server is unavailable. ' +
            'Make sure the backend is running.',
        );

        expect(testAccess.isSubmitting()).toBe(false);
    });

    it('should expose the submitting button label', () => {
        expect(testAccess.submitButtonLabel()).toBe(
            'Log in',
        );
    });
});