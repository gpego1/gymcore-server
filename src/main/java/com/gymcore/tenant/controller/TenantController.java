package com.gymcore.tenant.controller;

import java.util.UUID;
import jakarta.validation.Valid;
import com.gymcore.tenant.dto.TenantDtos.*;
import com.gymcore.tenant.service.TenantService;
import com.gymcore.shared.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academias")
@Tag(name = "academias")
public class TenantController {
    private final TenantService service;
    public TenantController(TenantService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar academias")
    public Response create(@Valid @RequestBody Request request) { return service.create(request); }

    @GetMapping
    @Operation(summary = "Listar academias", description = "Filtros opcionais por nome e ativo; page começa em zero, size de 1 a 100. Ordenação por UUID.")
    public PageResult<Response> list(@RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean active, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return service.list(name, active, page, size); }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar academias")
    public Response get(@PathVariable UUID id) { return service.get(id); }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cadastro de academias", description = "Atualiza os campos do cadastro; não reativa registros inativos.")
    public Response update(@PathVariable UUID id, @Valid @RequestBody Request request) { return service.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Inativar academias", description = "Exclusão lógica; preserva todos os vínculos e o histórico.")
    public void deactivate(@PathVariable UUID id) { service.deactivate(id); }
}
