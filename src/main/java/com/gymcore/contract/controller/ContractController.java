package com.gymcore.contract.controller;

import java.util.UUID;
import jakarta.validation.Valid;
import com.gymcore.contract.dto.ContractDtos.*;
import com.gymcore.contract.service.ContractService;
import com.gymcore.shared.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/academias/{tenantId}/contratos")
@Tag(name = "contratos")
public class ContractController {
    private final ContractService service;
    public ContractController(ContractService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Matricular com aceite presencial", description = "Cria contrato, snapshot dos termos e primeira mensalidade integral atomicamente. Término inclusivo.")
    public Response create(@PathVariable UUID tenantId, @Valid @RequestBody Request r) { return service.create(tenantId, r); }

    @GetMapping
    @Operation(summary = "Listar contratos", description = "Filtre por studentId; page >= 0, size de 1 a 100.")
    public PageResult<Response> list(@PathVariable UUID tenantId, @RequestParam(required = false) UUID studentId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.list(tenantId, studentId, page, size);
    }
    @PostMapping("/{id}/cancelamento")
    @Operation(summary = "Cancelar contrato", description = "Término inclusivo hoje ou futuro, sem apagar cobranças, sem multa e sem reembolso. Não estende vigência.")
    public Response cancel(@PathVariable UUID tenantId, @PathVariable UUID id, @Valid @RequestBody CancelRequest r) { return service.cancel(tenantId, id, r); }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar contratos por ID no contexto da academia")
    public Response get(@PathVariable UUID tenantId, @PathVariable UUID id) { return service.get(tenantId, id); }
}
