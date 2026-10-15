package dev.sameerkhan.payflow.payment.dto;

import dev.sameerkhan.payflow.payment.PaymentStatus;

import java.util.Map;

public record StatusSummary(Map<PaymentStatus, Long> counts, long total) {
}
