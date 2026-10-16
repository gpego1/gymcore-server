package com.gymcore.tenant.dto;

import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.gymcore.tenant.entity.Tenant;


public final class TenantDtos {
    private TenantDtos() {}

    public record Request(@NotBlank @Size(max = 120) @Schema(example = "Academia Exemplo") String name) {}
    public record Response(UUID id, String name, boolean active, Long version) {
        public static Response from(Tenant t) { return new Response(t.getId(), t.getName(), t.getActive(), t.getVersion()); }
    }
}
