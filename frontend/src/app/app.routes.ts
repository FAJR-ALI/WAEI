import { Routes } from '@angular/router';
import { authGuard, beneficiaryGuard, platformAdminGuard, providerGuard } from './core/guards';
import { Shell } from './layout/shell';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then((m) => m.LoginPage),
  },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.DashboardPage),
      },
      {
        path: 'catalog',
        canActivate: [beneficiaryGuard],
        loadComponent: () => import('./pages/catalog/catalog').then((m) => m.CatalogPage),
      },
      {
        path: 'requests',
        canActivate: [beneficiaryGuard],
        loadComponent: () => import('./pages/requests/requests').then((m) => m.BeneficiaryRequestsPage),
      },
      {
        path: 'provider/programs',
        canActivate: [providerGuard],
        loadComponent: () => import('./pages/provider/programs').then((m) => m.ProviderProgramsPage),
      },
      {
        path: 'provider/requests',
        canActivate: [providerGuard],
        loadComponent: () => import('./pages/provider/requests').then((m) => m.ProviderRequestsPage),
      },
      {
        path: 'admin/organizations',
        canActivate: [platformAdminGuard],
        loadComponent: () => import('./pages/admin/organizations').then((m) => m.AdminOrganizationsPage),
      },
      {
        path: 'admin/accounts',
        canActivate: [platformAdminGuard],
        loadComponent: () => import('./pages/admin/accounts').then((m) => m.AdminAccountsPage),
      },
      {
        path: 'admin/payments',
        canActivate: [platformAdminGuard],
        loadComponent: () => import('./pages/admin/payments').then((m) => m.AdminPaymentsPage),
      },
      {
        path: 'payment/callback',
        loadComponent: () =>
          import('./pages/payment-callback/payment-callback').then((m) => m.PaymentCallbackPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
