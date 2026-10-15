package dev.sameerkhan.payflow.payment.dto;

import dev.sameerkhan.payflow.payment.Approval;
import dev.sameerkhan.payflow.payment.Decision;
import dev.sameerkhan.payflow.payment.Payment;
import dev.sameerkhan.payflow.payment.PaymentStatus;
import dev.sameerkhan.payflow.payment.PaymentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * API view of a payment. Account numbers are always masked; the full
 * numbers never leave the service.
 */
public record PaymentResponse(
        Long id,
        String reference,
        PaymentType type,
        BigDecimal amount,
        String currency,
        String debitAccount,
        String beneficiaryName,
        String beneficiaryAccount,
        String memo,
        PaymentStatus status,
        int requiredApprovals,
        int approvalsReceived,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        boolean canDecide,
        List<ApprovalView> approvals
) {

    public record ApprovalView(String approver, Decision decision, String note, Instant decidedAt) {

        static ApprovalView from(Approval approval) {
            return new ApprovalView(approval.getApprover(), approval.getDecision(),
                    approval.getNote(), approval.getDecidedAt());
        }
    }

    public static PaymentResponse from(Payment p, String viewer, boolean viewerIsApprover) {
        return new PaymentResponse(
                p.getId(),
                p.getReference(),
                p.getType(),
                p.getAmount(),
                p.getCurrency(),
                mask(p.getDebitAccount()),
                p.getBeneficiaryName(),
                mask(p.getBeneficiaryAccount()),
                p.getMemo(),
                p.getStatus(),
                p.getRequiredApprovals(),
                p.approvalsReceived(),
                p.getCreatedBy(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                viewerIsApprover && p.canBeDecidedBy(viewer),
                p.getApprovals().stream().map(ApprovalView::from).toList());
    }

    static String mask(String account) {
        if (account == null || account.length() <= 4) {
            return "****";
        }
        return "****" + account.substring(account.length() - 4);
    }
}
