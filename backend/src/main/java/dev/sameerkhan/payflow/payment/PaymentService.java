package dev.sameerkhan.payflow.payment;

import dev.sameerkhan.payflow.common.NotFoundException;
import dev.sameerkhan.payflow.payment.dto.CreatePaymentRequest;
import dev.sameerkhan.payflow.payment.dto.PaymentResponse;
import dev.sameerkhan.payflow.payment.dto.StatusSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class PaymentService {

    private static final char[] REF_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final PaymentRepository payments;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public PaymentService(PaymentRepository payments, Clock clock) {
        this.payments = payments;
        this.clock = clock;
    }

    /**
     * Creates a payment. When an Idempotency-Key is supplied and the same user
     * already created a payment with it, the existing payment is returned instead
     * of a duplicate, so a retried request never initiates the same payment twice.
     */
    public CreateResult create(CreatePaymentRequest request, String user, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Payment> existing = payments.findByCreatedByAndIdempotencyKey(user, idempotencyKey);
            if (existing.isPresent()) {
                return new CreateResult(PaymentResponse.from(existing.get(), user, false), false);
            }
        }

        Payment payment = new Payment(newReference(), request.type(), request.amount(), request.currency(),
                request.debitAccount(), request.beneficiaryName().trim(), request.beneficiaryAccount(),
                request.memo(), user, blankToNull(idempotencyKey), clock.instant());
        if (request.submit()) {
            payment.submit(user, clock.instant());
        }
        return new CreateResult(PaymentResponse.from(payments.save(payment), user, false), true);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> list(PaymentStatus status, String viewer, boolean viewerIsApprover) {
        List<Payment> found = status == null
                ? payments.findAllByOrderByCreatedAtDesc()
                : payments.findByStatusOrderByCreatedAtDesc(status);
        return found.stream().map(p -> PaymentResponse.from(p, viewer, viewerIsApprover)).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(Long id, String viewer, boolean viewerIsApprover) {
        return PaymentResponse.from(load(id), viewer, viewerIsApprover);
    }

    @Transactional(readOnly = true)
    public StatusSummary summary() {
        Map<PaymentStatus, Long> counts = new EnumMap<>(PaymentStatus.class);
        for (PaymentStatus s : PaymentStatus.values()) {
            counts.put(s, 0L);
        }
        long total = 0;
        for (Object[] row : payments.countByStatus()) {
            long count = ((Number) row[1]).longValue();
            counts.put((PaymentStatus) row[0], count);
            total += count;
        }
        return new StatusSummary(counts, total);
    }

    public PaymentResponse submit(Long id, String user) {
        Payment payment = load(id);
        payment.submit(user, clock.instant());
        return PaymentResponse.from(payment, user, false);
    }

    public PaymentResponse decide(Long id, String approver, Decision decision, String note) {
        Payment payment = load(id);
        payment.recordDecision(approver, decision, note, clock.instant());
        payments.flush(); // surface optimistic-lock conflicts inside this request
        return PaymentResponse.from(payment, approver, true);
    }

    public PaymentResponse release(Long id, String user) {
        Payment payment = load(id);
        payment.release(clock.instant());
        return PaymentResponse.from(payment, user, false);
    }

    private Payment load(Long id) {
        return payments.findWithApprovalsById(id)
                .orElseThrow(() -> new NotFoundException("Payment " + id + " was not found"));
    }

    private String newReference() {
        StringBuilder sb = new StringBuilder("PF-");
        for (int i = 0; i < 8; i++) {
            sb.append(REF_ALPHABET[random.nextInt(REF_ALPHABET.length)]);
        }
        return sb.toString();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record CreateResult(PaymentResponse payment, boolean created) {
    }
}
