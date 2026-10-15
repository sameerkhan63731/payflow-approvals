import { createFeature, createReducer, on } from '@ngrx/store';
import { Payment, PaymentStatus, StatusSummary } from '../core/models';
import { PaymentsActions } from './payments.actions';

export interface PaymentsState {
  items: Payment[];
  summary: StatusSummary | null;
  filter: PaymentStatus | null;
  selected: Payment | null;
  loading: boolean;
  saving: boolean;
  error: string | null;
}

export const initialState: PaymentsState = {
  items: [],
  summary: null,
  filter: 'PENDING_APPROVAL',
  selected: null,
  loading: false,
  saving: false,
  error: null,
};

/** Replace a payment wherever it appears, keeping list and detail views consistent. */
function upsert(items: Payment[], payment: Payment): Payment[] {
  const exists = items.some((p) => p.id === payment.id);
  return exists ? items.map((p) => (p.id === payment.id ? payment : p)) : [payment, ...items];
}

export const paymentsReducer = createReducer(
  initialState,
  on(PaymentsActions.load, (state, { status }) => ({ ...state, filter: status, loading: true, error: null })),
  on(PaymentsActions.loadSuccess, (state, { payments, summary }) => ({
    ...state,
    items: payments,
    summary,
    loading: false,
  })),
  on(PaymentsActions.loadFailure, (state, { error }) => ({ ...state, loading: false, error })),

  on(PaymentsActions.open, (state, { id }) => ({
    ...state,
    selected: state.selected?.id === id ? state.selected : state.items.find((p) => p.id === id) ?? null,
    loading: true,
    error: null,
  })),
  on(PaymentsActions.openSuccess, (state, { payment }) => ({
    ...state,
    selected: payment,
    items: state.items.some((p) => p.id === payment.id) ? upsert(state.items, payment) : state.items,
    loading: false,
  })),
  on(PaymentsActions.openFailure, (state, { error }) => ({ ...state, loading: false, error })),

  on(PaymentsActions.decide, PaymentsActions.create, (state) => ({ ...state, saving: true, error: null })),
  on(PaymentsActions.decideSuccess, PaymentsActions.createSuccess, (state, { payment }) => ({
    ...state,
    saving: false,
    selected: payment,
    items: upsert(state.items, payment),
  })),
  on(PaymentsActions.decideFailure, PaymentsActions.createFailure, (state, { error }) => ({
    ...state,
    saving: false,
    error,
  })),

  on(PaymentsActions.clearError, (state) => ({ ...state, error: null })),
);

export const paymentsFeature = createFeature({
  name: 'payments',
  reducer: paymentsReducer,
});
