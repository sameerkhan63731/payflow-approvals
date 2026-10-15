package dev.sameerkhan.payflow.payment.dto;

import jakarta.validation.constraints.Size;

public record DecisionRequest(@Size(max = 280) String note) {
}
