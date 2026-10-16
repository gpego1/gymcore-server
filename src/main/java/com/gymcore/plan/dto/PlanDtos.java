package com.gymcore.plan.dto;

import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.gymcore.plan.entity.Plan;


public final class PlanDtos {
    private PlanDtos() {}

    public record Request(@NotBlank @Size(max = 120) @Schema(example = "Mensal") String name,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) @Schema(example = "120.00") BigDecimal monthlyAmount) {}
    public record Response(UUID id, UUID tenantId, String name, BigDecimal monthlyAmount, boolean active, Long version) {
        public static Response from(Plan p) { return new Response(p.getId(), p.getTenantId(), p.getName(), p.getMonthlyAmount(), p.getActive(), p.getVersion()); }
    }
}
