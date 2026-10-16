package com.gymcore.invoice.controller;

import java.util.UUID;
import jakarta.validation.Valid;
import com.gymcore.invoice.dto.InvoiceDtos.*;
import com.gymcore.invoice.service.InvoiceService;
import com.gymcore.shared.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.gymcore.invoice.enums.InvoiceStatus;

@RestController
@RequestMapping("/api/v1/academias/{tenantId}/mensalidades")
@Tag(name = "mensalidades")
public class InvoiceController {
    private final InvoiceService service;
    public InvoiceController(InvoiceService service) { this.service = service; }

    @PostMapping("/geracao")
    @Operation(summary = "Gerar mensalidades devidas", description = "Operação explícita e idempotente até throughDate, limitada a hoje. Inclui contratos encerrados com competências faltantes. GET nunca gera cobranças.")
    public GenerationResult generate(@PathVariable UUID tenantId, @Valid @RequestBody GenerateRequest r) { return service.generate(tenantId, r); }

    @GetMapping
    @Operation(summary = "Listar mensalidades", description = "Filtros opcionais por aluno, contrato, status e atraso. Page >= 0, size de 1 a 100.")
    public PageResult<Response> list(@PathVariable UUID tenantId, @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) UUID contractId, @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Boolean overdue, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return service.list(tenantId, studentId, contractId, status, overdue, page, size); }

    @PostMapping("/{id}/cancelamento")
    @Operation(summary = "Cancelar cobrança indevida", description = "Exige motivo e ausência de pagamento vigente; mantém o histórico como VOID.")
    public Response cancel(@PathVariable UUID tenantId, @PathVariable UUID id, @Valid @RequestBody VoidRequest r) { return service.cancel(tenantId, id, r); }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar mensalidades por ID no contexto da academia")
    public Response get(@PathVariable UUID tenantId, @PathVariable UUID id) { return service.get(tenantId, id); }
}
