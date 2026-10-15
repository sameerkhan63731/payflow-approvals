import { requiredApprovals } from './approval-policy';

describe('requiredApprovals', () => {
  it('matches the API tiers for ACH', () => {
    expect(requiredApprovals('ACH', 24_999.99)).toBe(1);
    expect(requiredApprovals('ACH', 25_000)).toBe(2);
    expect(requiredApprovals('ACH', 250_000)).toBe(3);
  });

  it('puts wires in higher tiers sooner', () => {
    expect(requiredApprovals('WIRE', 9_999.99)).toBe(1);
    expect(requiredApprovals('WIRE', 10_000)).toBe(2);
    expect(requiredApprovals('WIRE', 100_000)).toBe(3);
  });
});
