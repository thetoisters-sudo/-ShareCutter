import { Routes } from '@angular/router';

import { authGuard } from './core/auth/guards/auth.guard';
import { guestGuard } from './core/auth/guards/guest.guard';

export const routes: Routes = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
    },
    {
        path: 'dashboard',
        canActivate: [authGuard],
        loadComponent: () =>
            import(
                './features/dashboard/pages/dashboard-page/dashboard-page'
            ).then(
                (module) => module.DashboardPage,
            ),
        data: {
            title: 'Dashboard',
            description:
                'Review portfolio values, weekly targets and current allocation.',
        },
    },
    {
        path: 'portfolios',
        canActivate: [authGuard],
        loadComponent: () =>
            import(
                './features/portfolios/pages/portfolios-page/portfolios-page'
            ).then(
                (module) => module.PortfoliosPage,
            ),
        data: {
            title: 'Portfolios',
            description:
                'Create portfolios and manage their values, names and lifecycle.',
        },
    },
    {
        path: 'login',
        canActivate: [guestGuard],
        loadComponent: () =>
            import(
                './features/auth/pages/login/login-page'
            ).then(
                (module) => module.LoginPage,
            ),
    },
    {
        path: 'register',
        canActivate: [guestGuard],
        loadComponent: () =>
            import(
                './features/auth/pages/register/register-page'
            ).then(
                (module) => module.RegisterPage,
            ),
    },
    {
        path: '**',
        loadComponent: () =>
            import(
                './pages/placeholder-page/placeholder-page'
            ).then(
                (module) => module.PlaceholderPage,
            ),
        data: {
            title: 'Page not found',
            description:
                'The requested page does not exist.',
        },
    },
];