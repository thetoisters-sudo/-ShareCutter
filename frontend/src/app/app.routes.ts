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
            import(
                './pages/placeholder-page/placeholder-page'
            ).then(
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
            import(
                './pages/placeholder-page/placeholder-page'
            ).then(
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
            import(
                './features/auth/pages/login/login-page'
            ).then(
                (module) => module.LoginPage,
            ),
    },
    {
        path: 'register',
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