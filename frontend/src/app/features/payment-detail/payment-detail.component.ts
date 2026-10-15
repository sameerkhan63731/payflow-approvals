import { AsyncPipe, CurrencyPipe, DatePipe, NgFor, NgIf } from '@angular/common';
import { Component, Input, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Store } from '@ngrx/store';
import { AuthService } from '../../core/auth.service';
import { Payment, STATUS_LABELS } from '../../core/models';
import { DecisionKind, PaymentsActions } from '../../store/payments.actions';
import { selectError, selectSaving, selectSelected } from '../../store/payments.selectors';

@Component({
  selector: 'pf-payment-detail',
  standalone: true,
  imports: [NgIf, NgFor, AsyncPipe, CurrencyPipe, DatePipe, FormsModule, RouterLink],
  templateUrl: './payment-detail.component.html',
  styleUrls: ['./payment-detail.component.scss'],
})
export class PaymentDetailComponent {
  private readonly store = inject(Store);
  readonly auth = inject(AuthService);

  readonly labels = STATUS_LABELS;
  readonly payment$ = this.store.select(selectSelected);
  readonly saving$ = this.store.select(selectSaving);
  readonly error$ = this.store.select(selectError);

  note = '';
  private paymentId = 0;

  /** Bound from the :id route parameter. */
  @Input()
  set id(value: string) {
    this.paymentId = Number(value);
    this.note = '';
    this.store.dispatch(PaymentsActions.open({ id: this.paymentId }));
  }

  decide(kind: DecisionKind): void {
    this.store.dispatch(PaymentsActions.decide({ id: this.paymentId, kind, note: this.note.trim() }));
    this.note = '';
  }

  canRelease(p: Payment): boolean {
    return p.status === 'APPROVED' && this.auth.hasRole('ADMIN');
  }

  isMine(p: Payment): boolean {
    return p.createdBy === this.auth.session?.username;
  }

  remaining(p: Payment): number {
    return Math.max(p.requiredApprovals - p.approvalsReceived, 0);
  }

  dismissError(): void {
    this.store.dispatch(PaymentsActions.clearError());
  }
}
