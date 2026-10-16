package com.gymcore.plan.controller;

import java.util.UUID;
import jakarta.validation.Valid;
import com.gymcore.plan.dto.PlanDtos.*;
import com.gymcore.plan.service.PlanService;
import com.gymcore.shared.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academias/{tenantId}/planos")
@Tag(name = "planos")
public class PlanController {
    private final PlanService service;
    public PlanController(PlanService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar planos")
    public Response create(@PathVariable UUID tenantId, @Valid @RequestBody Request request) { return service.create(tenantId, request); }

    @GetMapping
    @Operation(summary = "Listar planos", description = "Filtros opcionais por nome e ativo; page começa em zero, size de 1 a 100. Ordenação por UUID.")
    public PageResult<Response> list(@PathVariable UUID tenantId, @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean active, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return service.list(tenantId, name, active, page, size); }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar planos")
    public Response get(@PathVariable UUID tenantId, @PathVariable UUID id) { return service.get(tenantId, id); }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cadastro de planos", description = "Atualiza os campos do cadastro; não reativa registros inativos.")
    public Response update(@PathVariable UUID tenantId, @PathVariable UUID id, @Valid @RequestBody Request request) { return service.update(tenantId, id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Inativar planos", description = "Exclusão lógica; preserva todos os vínculos e o histórico.")
    public void deactivate(@PathVariable UUID tenantId, @PathVariable UUID id) { service.deactivate(tenantId, id); }
}
