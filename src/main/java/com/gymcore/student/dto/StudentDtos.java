package com.gymcore.student.dto;

import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.gymcore.student.entity.Student;


public final class StudentDtos {
    private StudentDtos() {}

    public record Request(
            @NotBlank @Size(max = 120) @Schema(example = "Ana Exemplo") String name,
            @NotBlank @Email @Size(max = 254) @Schema(example = "ana@example.com") String email,
            @NotBlank @Pattern(regexp = "\\+[1-9][0-9]{9,14}") @Schema(example = "+5511999999999", description = "E.164: + e 10 a 15 dígitos") String phone,
            @NotNull @Schema(example = "1995-05-10", description = "Deve ter ao menos 18 anos na data comercial") LocalDate birthDate) {}
    public record Response(UUID id, UUID tenantId, String name, String email, String phone, LocalDate birthDate, boolean active, Long version) {
        public static Response from(Student s) { return new Response(s.getId(), s.getTenantId(), s.getName(), s.getEmail(), s.getPhone(), s.getBirthDate(), s.getActive(), s.getVersion()); }
    }
}
