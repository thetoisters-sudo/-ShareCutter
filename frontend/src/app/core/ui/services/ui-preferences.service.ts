import {
    DOCUMENT,
} from '@angular/common';
import {
    Injectable,
    computed,
    inject,
    signal,
} from '@angular/core';

export type ApplicationTheme =
    'light' | 'dark';

export type ApplicationLanguage =
    'en' | 'he';

const THEME_STORAGE_KEY =
    'sharecutter-theme';

const LANGUAGE_STORAGE_KEY =
    'sharecutter-language';

@Injectable({
    providedIn: 'root',
})
export class UiPreferencesService {
    private readonly document =
        inject(DOCUMENT);

    private readonly themeState =
        signal<ApplicationTheme>(
            this.resolveInitialTheme(),
        );

    private readonly languageState =
        signal<ApplicationLanguage>(
            this.resolveInitialLanguage(),
        );

    readonly theme =
        this.themeState.asReadonly();

    readonly language =
        this.languageState.asReadonly();

    readonly isDarkMode =
        computed(
            () =>
                this.themeState() ===
                'dark',
        );

    readonly isHebrew =
        computed(
            () =>
                this.languageState() ===
                'he',
        );

    readonly direction =
        computed(
            () =>
                this.isHebrew()
                    ? 'rtl'
                    : 'ltr',
        );

    constructor() {
        this.applyPreferences();
    }

    toggleTheme(): void {
        this.setTheme(
            this.isDarkMode()
                ? 'light'
                : 'dark',
        );
    }

    setTheme(
        theme: ApplicationTheme,
    ): void {
        this.themeState.set(theme);

        this.writeStorage(
            THEME_STORAGE_KEY,
            theme,
        );

        this.applyTheme();
    }

    toggleLanguage(): void {
        this.setLanguage(
            this.isHebrew()
                ? 'en'
                : 'he',
        );
    }

    setLanguage(
        language: ApplicationLanguage,
    ): void {
        this.languageState.set(
            language,
        );

        this.writeStorage(
            LANGUAGE_STORAGE_KEY,
            language,
        );

        this.applyLanguage();
    }

    private applyPreferences(): void {
        this.applyTheme();
        this.applyLanguage();
    }

    private applyTheme(): void {
        const root =
            this.document.documentElement;

        root.dataset['theme'] =
            this.themeState();

        root.style.colorScheme =
            this.themeState();
    }

    private applyLanguage(): void {
        const root =
            this.document.documentElement;

        root.lang =
            this.languageState();

        root.dir =
            this.direction();
    }

    private resolveInitialTheme():
        ApplicationTheme {
        const storedTheme =
            this.readStorage(
                THEME_STORAGE_KEY,
            );

        if (
            storedTheme === 'light' ||
            storedTheme === 'dark'
        ) {
            return storedTheme;
        }

        if (
            typeof window !==
            'undefined' &&
            typeof window.matchMedia ===
            'function' &&
            window.matchMedia(
                '(prefers-color-scheme: dark)',
            ).matches
        ) {
            return 'dark';
        }

        return 'light';
    }

    private resolveInitialLanguage():
        ApplicationLanguage {
        const storedLanguage =
            this.readStorage(
                LANGUAGE_STORAGE_KEY,
            );

        if (
            storedLanguage === 'en' ||
            storedLanguage === 'he'
        ) {
            return storedLanguage;
        }

        return 'en';
    }

    private readStorage(
        key: string,
    ): string | null {
        if (
            typeof window ===
            'undefined'
        ) {
            return null;
        }

        try {
            return window.localStorage
                .getItem(key);
        } catch {
            return null;
        }
    }

    private writeStorage(
        key: string,
        value: string,
    ): void {
        if (
            typeof window ===
            'undefined'
        ) {
            return;
        }

        try {
            window.localStorage
                .setItem(
                    key,
                    value,
                );
        } catch {
            // Storage availability must not
            // prevent the application from working.
        }
    }
}