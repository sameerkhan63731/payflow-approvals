package dev.sameerkhan.payflow.payment;

import java.math.BigDecimal;

/**
 * Decides how many distinct approvers a payment needs before it can be released.
 * Wires are riskier (same-day, hard to recall), so they hit higher tiers sooner than ACH.
 */
public final class ApprovalPolicy {

    private static final BigDecimal ACH_TIER_2 = new BigDecimal("25000.00");
    private static final BigDecimal ACH_TIER_3 = new BigDecimal("250000.00");
    private static final BigDecimal WIRE_TIER_2 = new BigDecimal("10000.00");
    private static final BigDecimal WIRE_TIER_3 = new BigDecimal("100000.00");

    private ApprovalPolicy() {
    }

    public static int requiredApprovals(PaymentType type, BigDecimal amount) {
        if (type == null || amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("type and a positive amount are required");
        }
        BigDecimal tier2 = type == PaymentType.WIRE ? WIRE_TIER_2 : ACH_TIER_2;
        BigDecimal tier3 = type == PaymentType.WIRE ? WIRE_TIER_3 : ACH_TIER_3;

        if (amount.compareTo(tier3) >= 0) {
            return 3;
        }
        if (amount.compareTo(tier2) >= 0) {
            return 2;
        }
        return 1;
    }
}
