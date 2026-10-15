package dev.sameerkhan.payflow.payment;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Seeds a handful of payments in different states so the dashboard has something to show. */
@Component
@Profile("demo")
public class DemoDataLoader implements ApplicationRunner {

    private final PaymentRepository payments;
    private final Clock clock;

    public DemoDataLoader(PaymentRepository payments, Clock clock) {
        this.payments = payments;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (payments.count() > 0) {
            return;
        }
        Instant now = clock.instant();

        Payment payroll = pending("PF-DEMO0001", PaymentType.ACH, "18450.00", "Northwind Payroll Services",
                "Bi-weekly payroll funding", now.minus(Duration.ofHours(5)));

        Payment supplier = pending("PF-DEMO0002", PaymentType.WIRE, "62500.00", "Contoso Steel Supply",
                "Invoice INV-20931", now.minus(Duration.ofHours(3)));
        supplier.recordDecision("raj", Decision.APPROVED, "Matches PO 4471", now.minus(Duration.ofHours(2)));

        Payment equipment = pending("PF-DEMO0003", PaymentType.WIRE, "240000.00", "Fabrikam Industrial",
                "Equipment purchase - 50% deposit", now.minus(Duration.ofHours(1)));

        Payment rent = pending("PF-DEMO0004", PaymentType.ACH, "9800.00", "Lakeside Properties LLC",
                "Office lease - November", now.minus(Duration.ofDays(1)));
        rent.recordDecision("lena", Decision.APPROVED, null, now.minus(Duration.ofHours(20)));

        Payment duplicate = pending("PF-DEMO0005", PaymentType.ACH, "4120.75", "Adatum Logistics",
                "Freight invoice 88812", now.minus(Duration.ofDays(2)));
        duplicate.recordDecision("raj", Decision.REJECTED, "Duplicate of a payment already sent on the 3rd",
                now.minus(Duration.ofDays(1)));

        payments.save(payroll);
        payments.save(supplier);
        payments.save(equipment);
        payments.save(rent);
        payments.save(duplicate);
    }

    private Payment pending(String ref, PaymentType type, String amount, String beneficiary, String memo, Instant at) {
        Payment p = new Payment(ref, type, new BigDecimal(amount), "USD", "000123456789", beneficiary,
                "4400918273", memo, "maya", null, at);
        p.submit("maya", at);
        return p;
    }
}
