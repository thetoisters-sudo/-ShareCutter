import { Routes } from '@angular/router';

export const routes: Routes = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
    },
    {
        path: 'dashboard',
        loadComponent: () =>
            import('./pages/placeholder-page/placeholder-page').then(
                (module) => module.PlaceholderPage,
            ),
        data: {
            title: 'Dashboard',
            description:
                'Review portfolio values, weekly targets and current allocation.',
        },
    },
    {
        path: 'portfolios',
        loadComponent: () =>
            import('./pages/placeholder-page/placeholder-page').then(
                (module) => module.PlaceholderPage,
            ),
        data: {
            title: 'Portfolios',
            description:
                'Create portfolios and manage assets, transactions and allocation targets.',
        },
    },
    {
        path: 'login',
        loadComponent: () =>
            import('./pages/placeholder-page/placeholder-page').then(
                (module) => module.PlaceholderPage,
            ),
        data: {
            title: 'Log in',
            description: 'Sign in to access your ShareCutter account.',
        },
    },
    {
        path: 'register',
        loadComponent: () =>
            import('./pages/placeholder-page/placeholder-page').then(
                (module) => module.PlaceholderPage,
            ),
        data: {
            title: 'Create account',
            description: 'Create an account and begin managing your portfolios.',
        },
    },
    {
        path: '**',
        loadComponent: () =>
            import('./pages/placeholder-page/placeholder-page').then(
                (module) => module.PlaceholderPage,
            ),
        data: {
            title: 'Page not found',
            description: 'The requested page does not exist.',
        },
    },
];