import { Payment, StatusSummary } from '../core/models';
import { PaymentsActions } from './payments.actions';
import { initialState, paymentsReducer } from './payments.reducer';

function payment(overrides: Partial<Payment> = {}): Payment {
  return {
    id: 1,
    reference: 'PF-TEST0001',
    type: 'WIRE',
    amount: 50000,
    currency: 'USD',
    debitAccount: '****6789',
    beneficiaryName: 'Contoso Steel',
    beneficiaryAccount: '****8273',
    memo: null,
    status: 'PENDING_APPROVAL',
    requiredApprovals: 2,
    approvalsReceived: 0,
    createdBy: 'maya',
    createdAt: '2026-10-01T15:00:00Z',
    updatedAt: '2026-10-01T15:00:00Z',
    canDecide: true,
    approvals: [],
    ...overrides,
  };
}

const summary: StatusSummary = {
  counts: { DRAFT: 0, PENDING_APPROVAL: 1, APPROVED: 0, REJECTED: 0, RELEASED: 0 },
  total: 1,
};

describe('paymentsReducer', () => {
  it('records the filter and loading state when a load starts', () => {
    const state = paymentsReducer(initialState, PaymentsActions.load({ status: 'APPROVED' }));
    expect(state.filter).toBe('APPROVED');
    expect(state.loading).toBeTrue();
    expect(state.error).toBeNull();
  });

  it('stores payments and summary on load success', () => {
    const loading = paymentsReducer(initialState, PaymentsActions.load({ status: null }));
    const state = paymentsReducer(loading, PaymentsActions.loadSuccess({ payments: [payment()], summary }));
    expect(state.items.length).toBe(1);
    expect(state.summary).toEqual(summary);
    expect(state.loading).toBeFalse();
  });

  it('updates the same payment in the list and the detail view after a decision', () => {
    const loaded = paymentsReducer(initialState, PaymentsActions.loadSuccess({ payments: [payment()], summary }));
    const deciding = paymentsReducer(loaded, PaymentsActions.decide({ id: 1, kind: 'approve', note: '' }));
    expect(deciding.saving).toBeTrue();

    const decided = payment({ approvalsReceived: 1, canDecide: false });
    const state = paymentsReducer(deciding, PaymentsActions.decideSuccess({ payment: decided }));

    expect(state.saving).toBeFalse();
    expect(state.items[0].approvalsReceived).toBe(1);
    expect(state.items[0].canDecide).toBeFalse();
    expect(state.selected).toEqual(decided);
  });

  it('keeps the error from a failed decision and clears it on request', () => {
    const failed = paymentsReducer(initialState, PaymentsActions.decideFailure({ error: 'Already decided' }));
    expect(failed.error).toBe('Already decided');
    expect(paymentsReducer(failed, PaymentsActions.clearError()).error).toBeNull();
  });

  it('shows the cached row immediately when opening a payment', () => {
    const loaded = paymentsReducer(initialState, PaymentsActions.loadSuccess({ payments: [payment({ id: 7 })], summary }));
    const state = paymentsReducer(loaded, PaymentsActions.open({ id: 7 }));
    expect(state.selected?.id).toBe(7);
    expect(state.loading).toBeTrue();
  });
});
