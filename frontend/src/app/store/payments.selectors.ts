import { createSelector } from '@ngrx/store';
import { paymentsFeature } from './payments.reducer';

export const {
  selectItems,
  selectSummary,
  selectFilter,
  selectSelected,
  selectLoading,
  selectSaving,
  selectError,
} = paymentsFeature;

/** Payments the signed-in approver can act on right now. */
export const selectAwaitingMe = createSelector(selectItems, (items) => items.filter((p) => p.canDecide));

/** Total value waiting for approval, grouped by currency. */
export const selectPendingTotals = createSelector(selectItems, (items) => {
  const totals: { currency: string; amount: number }[] = [];
  for (const p of items) {
    if (p.status !== 'PENDING_APPROVAL') {
      continue;
    }
    const row = totals.find((t) => t.currency === p.currency);
    if (row) {
      row.amount += p.amount;
    } else {
      totals.push({ currency: p.currency, amount: p.amount });
    }
  }
  return totals;
});
