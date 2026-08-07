import {
  ChangeDetectionStrategy,
  Component,
  inject,
} from '@angular/core';
import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';

import {
  AuthService,
} from './core/auth/services/auth.service';
import {
  UiPreferencesService,
} from './core/ui/services/ui-preferences.service';

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  changeDetection:
    ChangeDetectionStrategy.OnPush,
})
export class App {
  private readonly authService =
    inject(AuthService);

  private readonly router =
    inject(Router);

  private readonly uiPreferences =
    inject(UiPreferencesService);

  protected readonly applicationName =
    'ShareCutter';

  protected readonly currentUser =
    this.authService.currentUser;

  protected readonly isAuthenticated =
    this.authService.isAuthenticated;

  protected readonly isLoadingCurrentUser =
    this.authService
      .isLoadingCurrentUser;

  protected readonly isDarkMode =
    this.uiPreferences.isDarkMode;

  protected readonly isHebrew =
    this.uiPreferences.isHebrew;

  constructor() {
    this.initializeCurrentUser();
  }

  protected toggleTheme(): void {
    this.uiPreferences
      .toggleTheme();
  }

  protected toggleLanguage(): void {
    this.uiPreferences
      .toggleLanguage();
  }

  protected logout(): void {
    this.authService.logout();

    void this.router.navigate([
      '/login',
    ]);
  }

  private initializeCurrentUser(): void {
    if (
      !this.authService
        .hasValidStoredSession()
    ) {
      return;
    }

    this.authService
      .loadCurrentUser()
      .subscribe({
        error: () => {
          this.authService
            .logout();
        },
      });
  }
}