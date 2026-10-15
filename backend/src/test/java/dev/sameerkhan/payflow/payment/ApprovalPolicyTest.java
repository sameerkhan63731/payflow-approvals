package dev.sameerkhan.payflow.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApprovalPolicyTest {

    @ParameterizedTest(name = "{0} {1} needs {2} approval(s)")
    @CsvSource({
            "ACH,  0.01,       1",
            "ACH,  24999.99,   1",
            "ACH,  25000.00,   2",
            "ACH,  249999.99,  2",
            "ACH,  250000.00,  3",
            "WIRE, 9999.99,    1",
            "WIRE, 10000.00,   2",
            "WIRE, 99999.99,   2",
            "WIRE, 100000.00,  3"
    })
    void tiersByTypeAndAmount(PaymentType type, BigDecimal amount, int expected) {
        assertThat(ApprovalPolicy.requiredApprovals(type, amount)).isEqualTo(expected);
    }

    @Test
    void rejectsNonPositiveAmounts() {
        assertThatThrownBy(() -> ApprovalPolicy.requiredApprovals(PaymentType.ACH, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
