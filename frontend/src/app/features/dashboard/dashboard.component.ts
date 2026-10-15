import { AsyncPipe, CurrencyPipe, DatePipe, NgFor, NgIf } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Store } from '@ngrx/store';
import { AuthService } from '../../core/auth.service';
import { Payment, PaymentStatus, STATUSES, STATUS_LABELS } from '../../core/models';
import { PaymentsActions } from '../../store/payments.actions';
import {
  selectAwaitingMe,
  selectError,
  selectFilter,
  selectItems,
  selectLoading,
  selectPendingTotals,
  selectSummary,
} from '../../store/payments.selectors';

@Component({
  selector: 'pf-dashboard',
  standalone: true,
  imports: [NgIf, NgFor, AsyncPipe, CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit {
  private readonly store = inject(Store);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  readonly statuses = STATUSES;
  readonly labels = STATUS_LABELS;

  readonly items$ = this.store.select(selectItems);
  readonly summary$ = this.store.select(selectSummary);
  readonly filter$ = this.store.select(selectFilter);
  readonly loading$ = this.store.select(selectLoading);
  readonly error$ = this.store.select(selectError);
  readonly awaitingMe$ = this.store.select(selectAwaitingMe);
  readonly pendingTotals$ = this.store.select(selectPendingTotals);

  private currentFilter: PaymentStatus | null = 'PENDING_APPROVAL';

  ngOnInit(): void {
    this.filter$.subscribe((f) => (this.currentFilter = f)).unsubscribe();
    this.show(this.currentFilter);
  }

  show(status: PaymentStatus | null): void {
    this.store.dispatch(PaymentsActions.load({ status }));
  }

  open(payment: Payment): void {
    void this.router.navigate(['/payments', payment.id]);
  }

  dismissError(): void {
    this.store.dispatch(PaymentsActions.clearError());
  }

  steps(n: number): number[] {
    return Array.from({ length: n }, (_, i) => i);
  }

  trackById(_: number, p: Payment): number {
    return p.id;
  }
}
