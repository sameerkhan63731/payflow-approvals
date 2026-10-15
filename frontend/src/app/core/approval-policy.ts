import { PaymentType } from './models';

/**
 * Client-side mirror of the API's ApprovalPolicy, used only to preview the tier.
 * The server is the source of truth and recomputes it on create.
 */
const TIERS: Record<PaymentType, { two: number; three: number }> = {
  ACH: { two: 25_000, three: 250_000 },
  WIRE: { two: 10_000, three: 100_000 },
};

export function requiredApprovals(type: PaymentType, amount: number): number {
  const tier = TIERS[type];
  if (amount >= tier.three) {
    return 3;
  }
  if (amount >= tier.two) {
    return 2;
  }
  return 1;
}
