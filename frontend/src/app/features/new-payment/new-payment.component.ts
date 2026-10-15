import { AsyncPipe, NgIf } from '@angular/common';
import { Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Store } from '@ngrx/store';
import { PaymentType } from '../../core/models';
import { requiredApprovals } from '../../core/approval-policy';
import { PaymentsActions } from '../../store/payments.actions';
import { selectError, selectSaving } from '../../store/payments.selectors';

function newIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

type FieldName = 'amount' | 'currency' | 'debitAccount' | 'beneficiaryName' | 'beneficiaryAccount' | 'memo';

@Component({
  selector: 'pf-new-payment',
  standalone: true,
  imports: [ReactiveFormsModule, NgIf, AsyncPipe, RouterLink],
  templateUrl: './new-payment.component.html',
  styleUrls: ['./new-payment.component.scss'],
})
export class NewPaymentComponent {
  private readonly store = inject(Store);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly saving$ = this.store.select(selectSaving);
  readonly error$ = this.store.select(selectError);

  /** One key per form fill: retries and double-clicks reuse it, so the API creates one payment. */
  private readonly idempotencyKey = newIdempotencyKey();

  readonly form = this.fb.group({
    type: this.fb.control<PaymentType>('ACH', Validators.required),
    amount: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    currency: this.fb.control('USD', [Validators.required, Validators.pattern(/^[A-Z]{3}$/)]),
    debitAccount: this.fb.control('', [Validators.required, Validators.pattern(/^[0-9]{6,17}$/)]),
    beneficiaryName: this.fb.control('', [Validators.required, Validators.maxLength(140)]),
    beneficiaryAccount: this.fb.control('', [Validators.required, Validators.pattern(/^[0-9]{6,17}$/)]),
    memo: this.fb.control('', Validators.maxLength(140)),
  });

  /** Mirrors the server policy so the initiator sees how many approvals this will need. */
  get approvalsNeeded(): number | null {
    const { type, amount } = this.form.getRawValue();
    return amount && amount > 0 ? requiredApprovals(type, amount) : null;
  }

  showError(name: FieldName): boolean {
    const control = this.form.controls[name];
    return control.invalid && (control.touched || control.dirty);
  }

  submit(sendForApproval: boolean): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.store.dispatch(
      PaymentsActions.create({
        idempotencyKey: this.idempotencyKey,
        request: {
          type: v.type,
          amount: Math.round((v.amount ?? 0) * 100) / 100,
          currency: v.currency,
          debitAccount: v.debitAccount,
          beneficiaryName: v.beneficiaryName.trim(),
          beneficiaryAccount: v.beneficiaryAccount,
          memo: v.memo.trim() || null,
          submit: sendForApproval,
        },
      }),
    );
  }

  dismissError(): void {
    this.store.dispatch(PaymentsActions.clearError());
  }
}
