package com.gymcore.access.controller;

import java.util.UUID;
import jakarta.validation.Valid;
import com.gymcore.access.dto.AccessAttemptDtos.*;
import com.gymcore.access.service.AccessService;
import com.gymcore.shared.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.gymcore.access.enums.AccessDecision;

@RestController
@RequestMapping("/api/v1/academias/{tenantId}/acessos")
@Tag(name = "acessos")
public class AccessAttemptController {
    private final AccessService service;
    public AccessAttemptController(AccessService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Registrar decisão de acesso assistido", description = "Registra decisão sem operar dispositivo. Retorna 201 para permitido/negado ou 503 ProblemDetail com decisão persistida quando faltam mensalidades devidas.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Decisão registrada", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = Response.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "BILLING_SYNC_PENDING: execute a geração explícita", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/problem+json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public org.springframework.http.ResponseEntity<?> register(@PathVariable UUID tenantId, @Valid @RequestBody Request r,
            jakarta.servlet.http.HttpServletRequest request) {
        var decision = service.register(tenantId, r);
        if (decision.decision() == AccessDecision.UNAVAILABLE) {
            var p = org.springframework.http.ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Execute POST /api/v1/academias/" + tenantId + "/mensalidades/geracao antes de tentar novamente.");
            p.setInstance(java.net.URI.create(request.getRequestURI())); p.setProperty("code", "BILLING_SYNC_PENDING");
            p.setProperty("accessAttempt", decision);
            return org.springframework.http.ResponseEntity.status(503).body(p);
        }
        return org.springframework.http.ResponseEntity.status(201).body(decision);
    }
    @GetMapping
    @Operation(summary = "Listar decisões históricas", description = "Filtros opcionais por studentId e decision. Page >= 0, size de 1 a 100. Não altera decisões nem gera cobranças.")
    public PageResult<Response> list(@PathVariable UUID tenantId, @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) AccessDecision decision, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return service.list(tenantId, studentId, decision, page, size); }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar acessos por ID no contexto da academia")
    public Response get(@PathVariable UUID tenantId, @PathVariable UUID id) { return service.get(tenantId, id); }
}
