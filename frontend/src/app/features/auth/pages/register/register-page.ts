import {
    Component,
    computed,
    inject,
    signal,
} from '@angular/core';
import {
    AbstractControl,
    FormBuilder,
    ReactiveFormsModule,
    ValidationErrors,
    ValidatorFn,
    Validators,
} from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import {
    Router,
    RouterLink,
} from '@angular/router';
import { finalize } from 'rxjs';

import {
    ApiErrorResponse,
    RegisterRequest,
} from '../../../../core/auth/models/auth.models';
import { AuthService } from '../../../../core/auth/services/auth.service';

const matchingPasswordsValidator: ValidatorFn = (
    control: AbstractControl,
): ValidationErrors | null => {
    const password = control.get('password')?.value;
    const confirmPassword =
        control.get('confirmPassword')?.value;

    if (
        !password ||
        !confirmPassword ||
        password === confirmPassword
    ) {
        return null;
    }

    return {
        passwordsMismatch: true,
    };
};

@Component({
    selector: 'app-register-page',
    imports: [
        ReactiveFormsModule,
        RouterLink,
    ],
    templateUrl: './register-page.html',
    styleUrl: './register-page.scss',
})
export class RegisterPage {
    private readonly formBuilder = inject(FormBuilder);
    private readonly authService = inject(AuthService);
    private readonly router = inject(Router);

    protected readonly isSubmitting = signal(false);

    protected readonly errorMessage = signal<string | null>(
        null,
    );

    protected readonly registerForm =
        this.formBuilder.nonNullable.group(
            {
                firstName: [
                    '',
                    [
                        Validators.required,
                        Validators.maxLength(100),
                    ],
                ],
                lastName: [
                    '',
                    [
                        Validators.required,
                        Validators.maxLength(100),
                    ],
                ],
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
                        Validators.maxLength(72),
                    ],
                ],
                confirmPassword: [
                    '',
                    [
                        Validators.required,
                    ],
                ],
            },
            {
                validators: [
                    matchingPasswordsValidator,
                ],
            },
        );

    protected readonly firstNameControl =
        this.registerForm.controls.firstName;

    protected readonly lastNameControl =
        this.registerForm.controls.lastName;

    protected readonly emailControl =
        this.registerForm.controls.email;

    protected readonly passwordControl =
        this.registerForm.controls.password;

    protected readonly confirmPasswordControl =
        this.registerForm.controls.confirmPassword;

    protected readonly submitButtonLabel = computed(() =>
        this.isSubmitting()
            ? 'Creating account...'
            : 'Create account',
    );

    protected submit(): void {
        this.errorMessage.set(null);

        this.normalizeTextFields();

        if (this.registerForm.invalid) {
            this.registerForm.markAllAsTouched();
            return;
        }

        const formValue =
            this.registerForm.getRawValue();

        const request: RegisterRequest = {
            firstName: formValue.firstName,
            lastName: formValue.lastName,
            email: formValue.email,
            password: formValue.password,
        };

        this.isSubmitting.set(true);

        this.authService
            .register(request)
            .pipe(
                finalize(() => {
                    this.isSubmitting.set(false);
                }),
            )
            .subscribe({
                next: () => {
                    void this.router.navigate(
                        ['/login'],
                        {
                            queryParams: {
                                registered: 'true',
                            },
                        },
                    );
                },
                error: (error: unknown) => {
                    this.errorMessage.set(
                        this.resolveErrorMessage(error),
                    );
                },
            });
    }

    protected shouldShowFirstNameRequiredError(): boolean {
        return (
            this.firstNameControl.touched &&
            this.firstNameControl.hasError('required')
        );
    }

    protected shouldShowFirstNameLengthError(): boolean {
        return (
            this.firstNameControl.touched &&
            this.firstNameControl.hasError('maxlength')
        );
    }

    protected shouldShowLastNameRequiredError(): boolean {
        return (
            this.lastNameControl.touched &&
            this.lastNameControl.hasError('required')
        );
    }

    protected shouldShowLastNameLengthError(): boolean {
        return (
            this.lastNameControl.touched &&
            this.lastNameControl.hasError('maxlength')
        );
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

    protected shouldShowEmailLengthError(): boolean {
        return (
            this.emailControl.touched &&
            this.emailControl.hasError('maxlength')
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

    protected shouldShowConfirmPasswordRequiredError(): boolean {
        return (
            this.confirmPasswordControl.touched &&
            this.confirmPasswordControl.hasError('required')
        );
    }

    protected shouldShowPasswordMismatchError(): boolean {
        return (
            this.confirmPasswordControl.touched &&
            this.registerForm.hasError(
                'passwordsMismatch',
            )
        );
    }

    private normalizeTextFields(): void {
        this.firstNameControl.setValue(
            this.firstNameControl.value.trim(),
        );

        this.lastNameControl.setValue(
            this.lastNameControl.value.trim(),
        );

        this.emailControl.setValue(
            this.emailControl.value
                .trim()
                .toLowerCase(),
        );

        this.registerForm.updateValueAndValidity();
    }

    private resolveErrorMessage(
        error: unknown,
    ): string {
        if (!(error instanceof HttpErrorResponse)) {
            return 'Something went wrong. Please try again.';
        }

        if (error.status === 0) {
            return (
                'The ShareCutter server is unavailable. ' +
                'Make sure the backend is running.'
            );
        }

        const response =
            error.error as ApiErrorResponse | null;

        if (
            error.status === 409 ||
            this.containsExistingEmailMessage(response)
        ) {
            return (
                'An account with this email address ' +
                'already exists.'
            );
        }

        const validationMessage =
            this.resolveValidationMessage(response);

        if (validationMessage) {
            return validationMessage;
        }

        if (
            response &&
            typeof response.message === 'string' &&
            response.message.trim().length > 0
        ) {
            return response.message;
        }

        return 'Unable to create the account. Please try again.';
    }

    private containsExistingEmailMessage(
        response: ApiErrorResponse | null,
    ): boolean {
        if (
            !response ||
            typeof response.message !== 'string'
        ) {
            return false;
        }

        const message =
            response.message.toLowerCase();

        return (
            message.includes('email') &&
            (
                message.includes('exists') ||
                message.includes('already')
            )
        );
    }

    private resolveValidationMessage(
        response: ApiErrorResponse | null,
    ): string | null {
        const firstValidationError =
            response?.validationErrors?.[0];

        if (
            !firstValidationError ||
            typeof firstValidationError.message !== 'string'
        ) {
            return null;
        }

        return firstValidationError.message;
    }
}