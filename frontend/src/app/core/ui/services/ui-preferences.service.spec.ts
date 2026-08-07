import {
    DOCUMENT,
} from '@angular/common';
import {
    TestBed,
} from '@angular/core/testing';
import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest';

import {
    UiPreferencesService,
} from './ui-preferences.service';

describe(
    'UiPreferencesService',
    () => {
        let service:
            UiPreferencesService;

        let documentRef:
            Document;

        beforeEach(() => {
            window.localStorage.clear();

            Object.defineProperty(
                window,
                'matchMedia',
                {
                    configurable: true,
                    writable: true,
                    value:
                        vi.fn()
                            .mockReturnValue({
                                matches: false,
                                media: '',
                                onchange: null,
                                addListener:
                                    vi.fn(),
                                removeListener:
                                    vi.fn(),
                                addEventListener:
                                    vi.fn(),
                                removeEventListener:
                                    vi.fn(),
                                dispatchEvent:
                                    vi.fn(),
                            }),
                },
            );

            TestBed.configureTestingModule({
                providers: [
                    UiPreferencesService,
                ],
            });

            documentRef =
                TestBed.inject(DOCUMENT);

            documentRef
                .documentElement
                .removeAttribute(
                    'data-theme',
                );

            documentRef
                .documentElement
                .removeAttribute(
                    'lang',
                );

            documentRef
                .documentElement
                .removeAttribute(
                    'dir',
                );

            documentRef
                .documentElement
                .style
                .colorScheme = '';

            service =
                TestBed.inject(
                    UiPreferencesService,
                );
        });

        it(
            'should create the service',
            () => {
                expect(
                    service,
                ).toBeTruthy();
            },
        );

        it(
            'should use English and LTR by default',
            () => {
                expect(
                    service.language(),
                ).toBe('en');

                expect(
                    service.isHebrew(),
                ).toBe(false);

                expect(
                    service.direction(),
                ).toBe('ltr');

                expect(
                    documentRef
                        .documentElement
                        .lang,
                ).toBe('en');

                expect(
                    documentRef
                        .documentElement
                        .dir,
                ).toBe('ltr');
            },
        );

        it(
            'should switch to Hebrew and RTL',
            () => {
                service.toggleLanguage();

                expect(
                    service.language(),
                ).toBe('he');

                expect(
                    service.isHebrew(),
                ).toBe(true);

                expect(
                    service.direction(),
                ).toBe('rtl');

                expect(
                    documentRef
                        .documentElement
                        .lang,
                ).toBe('he');

                expect(
                    documentRef
                        .documentElement
                        .dir,
                ).toBe('rtl');

                expect(
                    window.localStorage
                        .getItem(
                            'sharecutter-language',
                        ),
                ).toBe('he');
            },
        );

        it(
            'should switch from Hebrew back to English',
            () => {
                service.setLanguage(
                    'he',
                );

                service.toggleLanguage();

                expect(
                    service.language(),
                ).toBe('en');

                expect(
                    service.isHebrew(),
                ).toBe(false);

                expect(
                    service.direction(),
                ).toBe('ltr');

                expect(
                    documentRef
                        .documentElement
                        .lang,
                ).toBe('en');

                expect(
                    documentRef
                        .documentElement
                        .dir,
                ).toBe('ltr');

                expect(
                    window.localStorage
                        .getItem(
                            'sharecutter-language',
                        ),
                ).toBe('en');
            },
        );

        it(
            'should restore a stored Hebrew preference',
            () => {
                TestBed.resetTestingModule();

                window.localStorage.setItem(
                    'sharecutter-language',
                    'he',
                );

                TestBed.configureTestingModule({
                    providers: [
                        UiPreferencesService,
                    ],
                });

                documentRef =
                    TestBed.inject(
                        DOCUMENT,
                    );

                service =
                    TestBed.inject(
                        UiPreferencesService,
                    );

                expect(
                    service.language(),
                ).toBe('he');

                expect(
                    service.direction(),
                ).toBe('rtl');

                expect(
                    documentRef
                        .documentElement
                        .lang,
                ).toBe('he');

                expect(
                    documentRef
                        .documentElement
                        .dir,
                ).toBe('rtl');
            },
        );

        it(
            'should use light theme by default',
            () => {
                expect(
                    service.theme(),
                ).toBe('light');

                expect(
                    service.isDarkMode(),
                ).toBe(false);

                expect(
                    documentRef
                        .documentElement
                        .dataset[
                    'theme'
                    ],
                ).toBe('light');

                expect(
                    documentRef
                        .documentElement
                        .style
                        .colorScheme,
                ).toBe('light');
            },
        );

        it(
            'should switch to dark theme and persist it',
            () => {
                service.toggleTheme();

                expect(
                    service.theme(),
                ).toBe('dark');

                expect(
                    service.isDarkMode(),
                ).toBe(true);

                expect(
                    documentRef
                        .documentElement
                        .dataset[
                    'theme'
                    ],
                ).toBe('dark');

                expect(
                    documentRef
                        .documentElement
                        .style
                        .colorScheme,
                ).toBe('dark');

                expect(
                    window.localStorage
                        .getItem(
                            'sharecutter-theme',
                        ),
                ).toBe('dark');
            },
        );

        it(
            'should switch from dark theme back to light theme',
            () => {
                service.setTheme(
                    'dark',
                );

                service.toggleTheme();

                expect(
                    service.theme(),
                ).toBe('light');

                expect(
                    service.isDarkMode(),
                ).toBe(false);

                expect(
                    documentRef
                        .documentElement
                        .dataset[
                    'theme'
                    ],
                ).toBe('light');

                expect(
                    window.localStorage
                        .getItem(
                            'sharecutter-theme',
                        ),
                ).toBe('light');
            },
        );

        it(
            'should restore a stored dark theme',
            () => {
                TestBed.resetTestingModule();

                window.localStorage.setItem(
                    'sharecutter-theme',
                    'dark',
                );

                TestBed.configureTestingModule({
                    providers: [
                        UiPreferencesService,
                    ],
                });

                documentRef =
                    TestBed.inject(
                        DOCUMENT,
                    );

                service =
                    TestBed.inject(
                        UiPreferencesService,
                    );

                expect(
                    service.theme(),
                ).toBe('dark');

                expect(
                    service.isDarkMode(),
                ).toBe(true);

                expect(
                    documentRef
                        .documentElement
                        .dataset[
                    'theme'
                    ],
                ).toBe('dark');

                expect(
                    documentRef
                        .documentElement
                        .style
                        .colorScheme,
                ).toBe('dark');
            },
        );
    },
);