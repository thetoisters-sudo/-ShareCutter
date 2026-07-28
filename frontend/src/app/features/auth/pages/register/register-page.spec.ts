import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import {
    ComponentFixture,
    TestBed,
} from '@angular/core/testing';
import { FormGroup } from '@angular/forms';
import {
    provideRouter,
    Router,
} from '@angular/router';
import {
    of,
    throwError,
} from 'rxjs';
import { vi } from 'vitest';

import {
    RegisterRequest,
    UserResponse,
} from '../../../../core/auth/models/auth.models';
import { AuthService } from '../../../../core/auth/services/auth.service';
import { RegisterPage } from './register-page';

@Component({
    selector: 'app-login-route-test-stub',
    standalone: true,
    template: '',
})
class LoginRouteTestStub {
}

interface RegisterPageTestAccess {
    registerForm: FormGroup;
    isSubmitting: () => boolean;
    errorMessage: () => string | null;
    submitButtonLabel: () => string;
    submit: () => void;
}

describe('RegisterPage', () => {
    let fixture: ComponentFixture<RegisterPage>;
    let component: RegisterPage;
    let testAccess: RegisterPageTestAccess;
    let router: Router;

    const registerMock = vi.fn();

    const authServiceMock = {
        register: registerMock,
    };

    const validFormValue = {
        firstName: 'Share',
        lastName: 'Cutter',
        email: 'user@example.com',
        password: 'password123',
        confirmPassword: 'password123',
    };

    const registerRequest: RegisterRequest = {
        firstName: 'Share',
        lastName: 'Cutter',
        email: 'user@example.com',
        password: 'password123',
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

    beforeEach(async () => {
        registerMock.mockReset();

        await TestBed.configureTestingModule({
            imports: [RegisterPage],
            providers: [
                provideRouter([
                    {
                        path: 'login',
                        component: LoginRouteTestStub,
                    },
                ]),
                {
                    provide: AuthService,
                    useValue: authServiceMock,
                },
            ],
        }).compileComponents();

        fixture =
            TestBed.createComponent(RegisterPage);

        component = fixture.componentInstance;

        testAccess =
            component as unknown as RegisterPageTestAccess;

        router = TestBed.inject(Router);

        fixture.detectChanges();
    });

    it('should create the registration page', () => {
        expect(component).toBeTruthy();
    });

    it('should render all registration fields', () => {
        const element =
            fixture.nativeElement as HTMLElement;

        expect(
            element.querySelector('#firstName'),
        ).toBeTruthy();

        expect(
            element.querySelector('#lastName'),
        ).toBeTruthy();

        expect(
            element.querySelector('#email'),
        ).toBeTruthy();

        expect(
            element.querySelector('#password'),
        ).toBeTruthy();

        expect(
            element.querySelector('#confirmPassword'),
        ).toBeTruthy();
    });

    it('should mark all controls as touched when submitted empty', () => {
        testAccess.submit();

        expect(testAccess.registerForm.invalid).toBe(
            true,
        );

        Object.values(
            testAccess.registerForm.controls,
        ).forEach((control) => {
            expect(control.touched).toBe(true);
        });

        expect(registerMock).not.toHaveBeenCalled();
    });

    it('should reject mismatched passwords', () => {
        testAccess.registerForm.setValue({
            ...validFormValue,
            confirmPassword: 'different123',
        });

        testAccess.submit();

        expect(
            testAccess.registerForm.hasError(
                'passwordsMismatch',
            ),
        ).toBe(true);

        expect(registerMock).not.toHaveBeenCalled();
    });

    it('should reject a password longer than 72 characters', () => {
        const longPassword = 'a'.repeat(73);

        testAccess.registerForm.setValue({
            ...validFormValue,
            password: longPassword,
            confirmPassword: longPassword,
        });

        testAccess.submit();

        expect(
            testAccess.registerForm.controls['password']
                .hasError('maxlength'),
        ).toBe(true);

        expect(registerMock).not.toHaveBeenCalled();
    });

    it('should register and navigate to login', () => {
        const navigateSpy = vi
            .spyOn(router, 'navigate')
            .mockResolvedValue(true);

        registerMock.mockReturnValue(
            of(userResponse),
        );

        testAccess.registerForm.setValue(
            validFormValue,
        );

        testAccess.submit();

        expect(registerMock).toHaveBeenCalledTimes(1);

        expect(registerMock).toHaveBeenCalledWith(
            registerRequest,
        );

        expect(navigateSpy).toHaveBeenCalledWith(
            ['/login'],
            {
                queryParams: {
                    registered: 'true',
                },
            },
        );

        expect(testAccess.isSubmitting()).toBe(false);
        expect(testAccess.errorMessage()).toBeNull();
    });

    it('should normalize names and email before registration', () => {
        registerMock.mockReturnValue(
            of(userResponse),
        );

        testAccess.registerForm.setValue({
            ...validFormValue,
            firstName: '  Share  ',
            lastName: '  Cutter  ',
            email: '  USER@EXAMPLE.COM  ',
        });

        testAccess.submit();

        expect(registerMock).toHaveBeenCalledWith({
            firstName: 'Share',
            lastName: 'Cutter',
            email: 'user@example.com',
            password: 'password123',
        });
    });

    it('should display an existing email message', () => {
        registerMock.mockReturnValue(
            throwError(
                () =>
                    new HttpErrorResponse({
                        status: 409,
                        statusText: 'Conflict',
                        error: {
                            message:
                                'A user with this email already exists',
                        },
                    }),
            ),
        );

        testAccess.registerForm.setValue(
            validFormValue,
        );

        testAccess.submit();

        expect(testAccess.errorMessage()).toBe(
            'An account with this email address ' +
            'already exists.',
        );

        expect(testAccess.isSubmitting()).toBe(false);
    });

    it('should display a backend validation message', () => {
        registerMock.mockReturnValue(
            throwError(
                () =>
                    new HttpErrorResponse({
                        status: 400,
                        statusText: 'Bad Request',
                        error: {
                            validationErrors: [
                                {
                                    field: 'email',
                                    message: 'Email must be valid',
                                },
                            ],
                        },
                    }),
            ),
        );

        testAccess.registerForm.setValue(
            validFormValue,
        );

        testAccess.submit();

        expect(testAccess.errorMessage()).toBe(
            'Email must be valid',
        );

        expect(testAccess.isSubmitting()).toBe(false);
    });

    it('should display a server unavailable message', () => {
        registerMock.mockReturnValue(
            throwError(
                () =>
                    new HttpErrorResponse({
                        status: 0,
                        statusText: 'Unknown Error',
                    }),
            ),
        );

        testAccess.registerForm.setValue(
            validFormValue,
        );

        testAccess.submit();

        expect(testAccess.errorMessage()).toBe(
            'The ShareCutter server is unavailable. ' +
            'Make sure the backend is running.',
        );
    });

    it('should expose the default submit button label', () => {
        expect(testAccess.submitButtonLabel()).toBe(
            'Create account',
        );
    });
});