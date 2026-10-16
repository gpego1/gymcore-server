package com.gymcore.payment.dto;

import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.gymcore.payment.entity.Payment;

import com.gymcore.payment.enums.*;

public final class PaymentDtos {
    private PaymentDtos() {}

    public record Request(@NotNull UUID invoiceId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) @Schema(example = "120.00") BigDecimal amount,
            @NotNull PaymentMethod method, @Size(max = 200) String reference,
            @NotBlank @Size(max = 120) @Schema(example = "Recepção Exemplo") String recordedBy) {}
    public record ReverseRequest(@NotBlank @Size(max = 500) String reason, @NotBlank @Size(max = 120) String recordedBy) {}
    public record Response(UUID id, UUID tenantId, UUID invoiceId, BigDecimal amount, PaymentMethod method,
            String reference, String recordedBy, Instant confirmedAt, Instant reversedAt, String reversedBy, String reversalReason, Long version) {
        public static Response from(Payment p) {
            return new Response(p.getId(), p.getTenantId(), p.getInvoiceId(), p.getAmount(), p.getMethod(), p.getReference(),
                p.getRecordedBy(), p.getConfirmedAt(), p.getReversedAt(), p.getReversedBy(), p.getReversalReason(), p.getVersion());
        }
    }
}
