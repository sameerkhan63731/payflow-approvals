import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'payments' },
  {
    path: 'login',
    title: 'Sign in · PayFlow',
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'payments',
    title: 'Payments · PayFlow',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: 'payments/new',
    title: 'New payment · PayFlow',
    canActivate: [roleGuard('INITIATOR')],
    loadComponent: () => import('./features/new-payment/new-payment.component').then((m) => m.NewPaymentComponent),
  },
  {
    path: 'payments/:id',
    title: 'Payment · PayFlow',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/payment-detail/payment-detail.component').then((m) => m.PaymentDetailComponent),
  },
  { path: '**', redirectTo: 'payments' },
];
