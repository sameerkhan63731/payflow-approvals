import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { CreatePaymentRequest, Payment, PaymentStatus, StatusSummary } from '../core/models';

export type DecisionKind = 'approve' | 'reject' | 'release';

export const PaymentsActions = createActionGroup({
  source: 'Payments',
  events: {
    'Load': props<{ status: PaymentStatus | null }>(),
    'Load Success': props<{ payments: Payment[]; summary: StatusSummary }>(),
    'Load Failure': props<{ error: string }>(),

    'Open': props<{ id: number }>(),
    'Open Success': props<{ payment: Payment }>(),
    'Open Failure': props<{ error: string }>(),

    'Decide': props<{ id: number; kind: DecisionKind; note: string }>(),
    'Decide Success': props<{ payment: Payment }>(),
    'Decide Failure': props<{ error: string }>(),

    'Create': props<{ request: CreatePaymentRequest; idempotencyKey: string }>(),
    'Create Success': props<{ payment: Payment }>(),
    'Create Failure': props<{ error: string }>(),

    'Clear Error': emptyProps(),
  },
});
