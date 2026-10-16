package com.gymcore.access.dto;

import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.gymcore.access.entity.AccessAttempt;

import com.gymcore.access.enums.*;

public final class AccessAttemptDtos {
    private AccessAttemptDtos() {}

    public record Request(@NotNull UUID studentId, @NotBlank @Size(max = 120) @Schema(example = "Recepção Exemplo") String recordedBy) {}
    public record Response(UUID id, UUID tenantId, UUID studentId, String recordedBy, Instant occurredAt, AccessDecision decision, AccessReason reason) {
        public static Response from(AccessAttempt a) { return new Response(a.getId(), a.getTenantId(), a.getStudentId(), a.getRecordedBy(), a.getOccurredAt(), a.getDecision(), a.getReason()); }
    }
}
