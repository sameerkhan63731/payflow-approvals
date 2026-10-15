import { Payment } from '../core/models';
import { selectAwaitingMe, selectPendingTotals } from './payments.selectors';

function p(id: number, overrides: Partial<Payment>): Payment {
  return {
    id,
    reference: `PF-${id}`,
    type: 'ACH',
    amount: 100,
    currency: 'USD',
    debitAccount: '****0001',
    beneficiaryName: 'Beneficiary',
    beneficiaryAccount: '****0002',
    memo: null,
    status: 'PENDING_APPROVAL',
    requiredApprovals: 1,
    approvalsReceived: 0,
    createdBy: 'maya',
    createdAt: '2026-10-01T15:00:00Z',
    updatedAt: '2026-10-01T15:00:00Z',
    canDecide: false,
    approvals: [],
    ...overrides,
  };
}

describe('payments selectors', () => {
  const items = [
    p(1, { canDecide: true, amount: 1000 }),
    p(2, { amount: 250.5 }),
    p(3, { currency: 'EUR', amount: 400 }),
    p(4, { status: 'APPROVED', amount: 9999 }),
  ];

  it('lists only payments the current user can act on', () => {
    expect(selectAwaitingMe.projector(items).map((x: Payment) => x.id)).toEqual([1]);
  });

  it('totals pending amounts per currency and ignores other statuses', () => {
    expect(selectPendingTotals.projector(items)).toEqual([
      { currency: 'USD', amount: 1250.5 },
      { currency: 'EUR', amount: 400 },
    ]);
  });
});
