package dev.sameerkhan.payflow.payment;

import dev.sameerkhan.payflow.common.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private static final Instant NOW = Instant.parse("2026-10-01T15:00:00Z");

    private static Payment pendingWire(String amount) {
        Payment p = new Payment("PF-TEST0001", PaymentType.WIRE, new BigDecimal(amount), "USD",
                "000123456789", "Contoso", "4400918273", null, "maya", null, NOW);
        p.submit("maya", NOW);
        return p;
    }

    @Test
    void newPaymentStartsAsDraftWithPolicyTier() {
        Payment p = new Payment("PF-TEST0001", PaymentType.WIRE, new BigDecimal("50000.00"), "USD",
                "000123456789", "Contoso", "4400918273", null, "maya", null, NOW);

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.DRAFT);
        assertThat(p.getRequiredApprovals()).isEqualTo(2);
    }

    @Test
    void onlyTheInitiatorCanSubmit() {
        Payment p = new Payment("PF-TEST0001", PaymentType.ACH, new BigDecimal("100.00"), "USD",
                "000123456789", "Contoso", "4400918273", null, "maya", null, NOW);

        assertThatThrownBy(() -> p.submit("raj", NOW))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("initiator");
    }

    @Test
    void becomesApprovedOnlyWhenTheTierIsMet() {
        Payment p = pendingWire("50000.00"); // 2 approvals required

        p.recordDecision("raj", Decision.APPROVED, null, NOW);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PENDING_APPROVAL);
        assertThat(p.approvalsReceived()).isEqualTo(1);

        p.recordDecision("lena", Decision.APPROVED, null, NOW);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    void initiatorCannotApproveTheirOwnPayment() {
        Payment p = pendingWire("500.00");

        assertThatThrownBy(() -> p.recordDecision("maya", Decision.APPROVED, null, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode())
                .isEqualTo("MAKER_CHECKER");
        assertThat(p.canBeDecidedBy("maya")).isFalse();
    }

    @Test
    void sameApproverCannotCountTwice() {
        Payment p = pendingWire("50000.00");
        p.recordDecision("raj", Decision.APPROVED, null, NOW);

        assertThatThrownBy(() -> p.recordDecision("raj", Decision.APPROVED, null, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode())
                .isEqualTo("ALREADY_DECIDED");
        assertThat(p.canBeDecidedBy("raj")).isFalse();
        assertThat(p.canBeDecidedBy("lena")).isTrue();
    }

    @Test
    void rejectionNeedsAReasonAndIsFinal() {
        Payment p = pendingWire("50000.00");

        assertThatThrownBy(() -> p.recordDecision("raj", Decision.REJECTED, "  ", NOW))
                .isInstanceOf(BusinessRuleException.class);

        p.recordDecision("raj", Decision.REJECTED, "Beneficiary details do not match invoice", NOW);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.REJECTED);

        assertThatThrownBy(() -> p.recordDecision("lena", Decision.APPROVED, null, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("REJECTED");
    }

    @Test
    void draftsCannotBeApprovedAndOnlyApprovedPaymentsRelease() {
        Payment draft = new Payment("PF-TEST0001", PaymentType.ACH, new BigDecimal("100.00"), "USD",
                "000123456789", "Contoso", "4400918273", null, "maya", null, NOW);
        assertThatThrownBy(() -> draft.recordDecision("raj", Decision.APPROVED, null, NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> draft.release(NOW))
                .isInstanceOf(BusinessRuleException.class);

        Payment small = pendingWire("500.00");
        small.recordDecision("raj", Decision.APPROVED, null, NOW);
        small.release(NOW);
        assertThat(small.getStatus()).isEqualTo(PaymentStatus.RELEASED);
    }
}
