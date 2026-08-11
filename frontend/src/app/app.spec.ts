import {
  signal,
} from '@angular/core';
import {
  ComponentFixture,
  TestBed,
} from '@angular/core/testing';
import {
  Router,
  provideRouter,
} from '@angular/router';
import {
  of,
} from 'rxjs';
import {
  vi,
} from 'vitest';

import {
  App,
} from './app';
import {
  UserResponse,
} from './core/auth/models/auth.models';
import {
  AuthService,
} from './core/auth/services/auth.service';

describe('App', () => {
  const currentUserState =
    signal<UserResponse | null>(null);

  const authenticatedState =
    signal(false);

  const loadingState =
    signal(false);

  const authServiceMock = {
    currentUser:
      currentUserState.asReadonly(),

    isAuthenticated:
      authenticatedState.asReadonly(),

    isLoadingCurrentUser:
      loadingState.asReadonly(),

    hasValidStoredSession:
      vi.fn(() => false),

    loadCurrentUser:
      vi.fn(() => of(createUser())),

    logout:
      vi.fn(),
  };

  let fixture:
    ComponentFixture<App>;

  let router:
    Router;

  beforeEach(async () => {
    currentUserState.set(null);
    authenticatedState.set(false);
    loadingState.set(false);

    authServiceMock
      .hasValidStoredSession
      .mockReset();

    authServiceMock
      .hasValidStoredSession
      .mockReturnValue(false);

    authServiceMock
      .loadCurrentUser
      .mockReset();

    authServiceMock
      .loadCurrentUser
      .mockReturnValue(
        of(createUser()),
      );

    authServiceMock
      .logout
      .mockReset();

    await TestBed.configureTestingModule({
      imports: [
        App,
      ],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: authServiceMock,
        },
      ],
    }).compileComponents();

    router =
      TestBed.inject(Router);

    fixture =
      TestBed.createComponent(App);
  });

  it('should create the application', () => {
    expect(
      fixture.componentInstance,
    ).toBeTruthy();
  });

  it('should render the ShareCutter brand', () => {
    fixture.detectChanges();

    const compiled =
      fixture.nativeElement as HTMLElement;

    const brandName =
      compiled.querySelector(
        '.brand__text strong',
      );

    expect(
      brandName?.textContent?.trim(),
    ).toBe('ShareCutter');
  });

  it('should render the primary navigation links', () => {
    fixture.detectChanges();

    const compiled =
      fixture.nativeElement as HTMLElement;

    const navigationLinks =
      Array.from(
        compiled.querySelectorAll<
          HTMLAnchorElement
        >(
          '.main-navigation .main-navigation__link',
        ),
      ).map(
        (link) =>
          link.textContent?.trim(),
      );

    expect(navigationLinks).toEqual([
      'Dashboard',
      'Portfolios',
      'Assets',
      'Transactions',
      'Allocation',
    ]);
  });

  it('should link the Assets navigation item to the assets page', () => {
    fixture.detectChanges();

    const compiled =
      fixture.nativeElement as HTMLElement;

    const assetsLink =
      compiled.querySelector<
        HTMLAnchorElement
      >(
        'a[routerLink="/assets"]',
      );

    expect(
      assetsLink,
    ).not.toBeNull();

    expect(
      assetsLink?.textContent?.trim(),
    ).toBe('Assets');
  });

  it('should link the Transactions navigation item to the transactions page', () => {
    fixture.detectChanges();

    const compiled =
      fixture.nativeElement as HTMLElement;

    const transactionsLink =
      compiled.querySelector<
        HTMLAnchorElement
      >(
        'a[routerLink="/transactions"]',
      );

    expect(
      transactionsLink,
    ).not.toBeNull();

    expect(
      transactionsLink
        ?.textContent
        ?.trim(),
    ).toBe('Transactions');
  });

  it('should link the Allocation navigation item to the allocation page', () => {
    fixture.detectChanges();

    const compiled =
      fixture.nativeElement as HTMLElement;

    const allocationLink =
      compiled.querySelector<
        HTMLAnchorElement
      >(
        'a[routerLink="/allocation-purchase"]',
      );

    expect(
      allocationLink,
    ).not.toBeNull();

    expect(
      allocationLink
        ?.textContent
        ?.trim(),
    ).toBe('Allocation');
  });

  it('should render guest account actions when logged out', () => {
    fixture.detectChanges();

    const compiled =
      fixture.nativeElement as HTMLElement;

    const loginLink =
      compiled.querySelector<
        HTMLAnchorElement
      >(
        'a[routerLink="/login"]',
      );

    const registerLink =
      compiled.querySelector<
        HTMLAnchorElement
      >(
        'a[routerLink="/register"]',
      );

    expect(
      loginLink?.textContent?.trim(),
    ).toBe('Log in');

    expect(
      registerLink
        ?.textContent
        ?.trim(),
    ).toBe('Create account');

    expect(
      compiled.querySelector(
        '.account-navigation__logout',
      ),
    ).toBeNull();
  });

  it('should load the current user when a valid session exists', () => {
    authServiceMock
      .hasValidStoredSession
      .mockReturnValue(true);

    fixture =
      TestBed.createComponent(App);

    expect(
      authServiceMock.loadCurrentUser,
    ).toHaveBeenCalledTimes(1);
  });

  it('should not load the current user without a valid session', () => {
    expect(
      authServiceMock.loadCurrentUser,
    ).not.toHaveBeenCalled();
  });

  it('should render the authenticated user identity', () => {
    const user =
      createUser();

    currentUserState.set(user);
    authenticatedState.set(true);

    fixture.detectChanges();

    const compiled =
      fixture.nativeElement as HTMLElement;

    const userName =
      compiled.querySelector(
        '.account-navigation__user strong',
      );

    const userEmail =
      compiled.querySelector(
        '.account-navigation__user small',
      );

    const logoutButton =
      compiled.querySelector<
        HTMLButtonElement
      >(
        '.account-navigation__logout',
      );

    expect(
      userName?.textContent?.trim(),
    ).toBe('Dana Cohen');

    expect(
      userEmail?.textContent?.trim(),
    ).toBe('dana@example.com');

    expect(
      logoutButton
        ?.textContent
        ?.trim(),
    ).toBe('Log out');

    expect(
      compiled.querySelector(
        'a[routerLink="/login"]',
      ),
    ).toBeNull();
  });

  it('should render the account loading state', () => {
    loadingState.set(true);

    fixture.detectChanges();

    const compiled =
      fixture.nativeElement as HTMLElement;

    const loadingMessage =
      compiled.querySelector(
        '.account-navigation__loading',
      );

    expect(
      loadingMessage
        ?.textContent
        ?.trim(),
    ).toBe('Loading account...');
  });

  it('should log out and navigate to login', async () => {
    currentUserState.set(
      createUser(),
    );

    authenticatedState.set(true);

    const navigateSpy =
      vi.spyOn(
        router,
        'navigate',
      ).mockResolvedValue(true);

    fixture.detectChanges();

    const compiled =
      fixture.nativeElement as HTMLElement;

    const logoutButton =
      compiled.querySelector<
        HTMLButtonElement
      >(
        '.account-navigation__logout',
      );

    logoutButton?.click();

    expect(
      authServiceMock.logout,
    ).toHaveBeenCalledTimes(1);

    expect(
      navigateSpy,
    ).toHaveBeenCalledWith([
      '/login',
    ]);
  });
});

function createUser():
  UserResponse {
  return {
    id:
      '62df8d45-74ef-4ba8-a302-a33cc33b5b21',

    email:
      'dana@example.com',

    firstName:
      'Dana',

    lastName:
      'Cohen',

    role:
      'USER',

    status:
      'ACTIVE',

    createdAt:
      '2026-07-30T09:00:00Z',

    updatedAt:
      '2026-07-30T09:00:00Z',
  };
}