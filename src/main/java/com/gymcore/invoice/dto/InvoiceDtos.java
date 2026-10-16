package com.gymcore.invoice.dto;

import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.gymcore.invoice.entity.Invoice;

import com.gymcore.invoice.enums.*;

public final class InvoiceDtos {
    private InvoiceDtos() {}

    public record GenerateRequest(@NotNull @Schema(description = "Não pode superar a data comercial atual", example = "2026-10-16") LocalDate throughDate) {}
    public record GenerationResult(int created) {}
    public record VoidRequest(@NotBlank @Size(max = 500) String reason) {}
    public record Response(UUID id, UUID tenantId, UUID contractId, LocalDate competence, LocalDate dueDate,
            BigDecimal amount, InvoiceStatus status, String voidReason, boolean overdue, Long version) {
        public static Response from(Invoice i, LocalDate today) {
            return new Response(i.getId(), i.getTenantId(), i.getContractId(), i.getCompetence(), i.getDueDate(),
                i.getAmount(), i.getStatus(), i.getVoidReason(), i.getStatus() == InvoiceStatus.OPEN && i.getDueDate().isBefore(today), i.getVersion());
        }
    }
}
