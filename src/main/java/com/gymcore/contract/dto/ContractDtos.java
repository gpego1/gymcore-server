package com.gymcore.contract.dto;

import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.gymcore.contract.entity.Contract;
import com.gymcore.acceptance.entity.ContractAcceptance;

public final class ContractDtos {
    private ContractDtos() {}

    public record Request(@NotNull UUID studentId, @NotNull UUID planId,
            @NotNull @Schema(example = "2026-10-16") LocalDate startDate,
            @Schema(description = "Último dia de vigência, inclusivo; null significa sem término") LocalDate endDate,
            @NotBlank @Schema(example = "2026-10-v1") String termsVersion,
            @NotBlank @Size(max = 120) @Schema(example = "Ana Exemplo") String acceptedName,
            @NotBlank @Size(max = 120) @Schema(example = "Recepção Exemplo") String recordedBy) {}
    public record CancelRequest(@NotNull LocalDate endDate, @NotBlank @Size(max = 500) String reason) {}
    public record Acceptance(String termsVersion, String termsText, String acceptedName, String recordedBy, Instant occurredAt) {}
    public record Response(UUID id, UUID tenantId, UUID studentId, UUID planId, LocalDate startDate, LocalDate endDate,
            BigDecimal amountSnapshot, String cancellationReason, Long version, Acceptance acceptance) {
        public static Response from(Contract c, ContractAcceptance a) {
            return new Response(c.getId(), c.getTenantId(), c.getStudentId(), c.getPlanId(), c.getStartDate(), c.getEndDate(),
                c.getAmountSnapshot(), c.getCancellationReason(), c.getVersion(),
                new Acceptance(a.getTermsVersion(), a.getTermsText(), a.getAcceptedName(), a.getRecordedBy(), a.getOccurredAt()));
        }
    }
}
