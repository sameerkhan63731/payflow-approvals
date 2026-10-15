package dev.sameerkhan.payflow.payment;

import dev.sameerkhan.payflow.common.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate root for a payment and its approval trail.
 * All workflow rules live here so they are enforced the same way
 * no matter which API or job changes the payment.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 10)
    private PaymentType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "debit_account", nullable = false, length = 34)
    private String debitAccount;

    @Column(name = "beneficiary_name", nullable = false, length = 140)
    private String beneficiaryName;

    @Column(name = "beneficiary_account", nullable = false, length = 34)
    private String beneficiaryAccount;

    @Column(length = 140)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "required_approvals", nullable = false)
    private int requiredApprovals;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("decidedAt ASC")
    private List<Approval> approvals = new ArrayList<>();

    protected Payment() {
        // for JPA
    }

    public Payment(String reference, PaymentType type, BigDecimal amount, String currency,
                   String debitAccount, String beneficiaryName, String beneficiaryAccount,
                   String memo, String createdBy, String idempotencyKey, Instant now) {
        this.reference = reference;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.debitAccount = debitAccount;
        this.beneficiaryName = beneficiaryName;
        this.beneficiaryAccount = beneficiaryAccount;
        this.memo = memo;
        this.createdBy = createdBy;
        this.idempotencyKey = idempotencyKey;
        this.requiredApprovals = ApprovalPolicy.requiredApprovals(type, amount);
        this.status = PaymentStatus.DRAFT;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void submit(String user, Instant now) {
        if (!createdBy.equals(user)) {
            throw new BusinessRuleException("NOT_INITIATOR", "Only the initiator can submit this payment");
        }
        requireStatus(PaymentStatus.DRAFT, "submitted");
        status = PaymentStatus.PENDING_APPROVAL;
        updatedAt = now;
    }

    public void recordDecision(String approver, Decision decision, String note, Instant now) {
        requireStatus(PaymentStatus.PENDING_APPROVAL, "approved or rejected");
        if (createdBy.equals(approver)) {
            throw new BusinessRuleException("MAKER_CHECKER",
                    "The initiator of a payment cannot approve or reject it");
        }
        if (hasDecided(approver)) {
            throw new BusinessRuleException("ALREADY_DECIDED",
                    "You have already recorded a decision on this payment");
        }
        if (decision == Decision.REJECTED && (note == null || note.isBlank())) {
            throw new BusinessRuleException("NOTE_REQUIRED", "A reason is required to reject a payment");
        }

        approvals.add(new Approval(this, approver, decision, note, now));
        updatedAt = now;

        if (decision == Decision.REJECTED) {
            status = PaymentStatus.REJECTED;
        } else if (approvalsReceived() >= requiredApprovals) {
            status = PaymentStatus.APPROVED;
        }
    }

    public void release(Instant now) {
        requireStatus(PaymentStatus.APPROVED, "released");
        status = PaymentStatus.RELEASED;
        updatedAt = now;
    }

    public boolean canBeDecidedBy(String user) {
        return status == PaymentStatus.PENDING_APPROVAL && !createdBy.equals(user) && !hasDecided(user);
    }

    public int approvalsReceived() {
        return (int) approvals.stream().filter(a -> a.getDecision() == Decision.APPROVED).count();
    }

    private boolean hasDecided(String user) {
        return approvals.stream().anyMatch(a -> a.getApprover().equals(user));
    }

    private void requireStatus(PaymentStatus expected, String action) {
        if (status != expected) {
            throw new BusinessRuleException("INVALID_STATUS",
                    "A payment in status " + status + " cannot be " + action);
        }
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public PaymentType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDebitAccount() {
        return debitAccount;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public String getBeneficiaryAccount() {
        return beneficiaryAccount;
    }

    public String getMemo() {
        return memo;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public int getRequiredApprovals() {
        return requiredApprovals;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Approval> getApprovals() {
        return Collections.unmodifiableList(approvals);
    }
}
