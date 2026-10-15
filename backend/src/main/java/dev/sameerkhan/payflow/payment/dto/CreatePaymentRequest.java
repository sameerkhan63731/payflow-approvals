package dev.sameerkhan.payflow.payment.dto;

import dev.sameerkhan.payflow.payment.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull PaymentType type,
        @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO currency code") String currency,
        @NotBlank @Pattern(regexp = "[0-9]{6,17}", message = "must be 6-17 digits") String debitAccount,
        @NotBlank @Size(max = 140) String beneficiaryName,
        @NotBlank @Pattern(regexp = "[0-9]{6,17}", message = "must be 6-17 digits") String beneficiaryAccount,
        @Size(max = 140) String memo,
        boolean submit
) {
}
