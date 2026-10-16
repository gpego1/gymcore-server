package com.gymcore.payment.controller;

import java.util.UUID;
import jakarta.validation.Valid;
import com.gymcore.payment.dto.PaymentDtos.*;
import com.gymcore.payment.service.PaymentService;
import com.gymcore.shared.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/academias/{tenantId}/pagamentos")
@Tag(name = "pagamentos")
public class PaymentController {
    private final PaymentService service;
    public PaymentController(PaymentService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar pagamento manual integral", description = "Registra recebimento externo, sem processar dinheiro. Apenas um pagamento vigente por mensalidade.")
    public Response create(@PathVariable UUID tenantId, @Valid @RequestBody Request r) { return service.create(tenantId, r); }

    @GetMapping
    @Operation(summary = "Listar pagamentos", description = "Filtros opcionais por invoiceId e reversed. Page >= 0, size de 1 a 100.")
    public PageResult<Response> list(@PathVariable UUID tenantId, @RequestParam(required = false) UUID invoiceId,
            @RequestParam(required = false) Boolean reversed, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return service.list(tenantId, invoiceId, reversed, page, size); }

    @PostMapping("/{id}/estorno")
    @Operation(summary = "Estornar registro de pagamento", description = "Exige motivo e operador. Preserva pagamento e reabre somente a cobrança correspondente; não realiza reembolso bancário.")
    public Response reverse(@PathVariable UUID tenantId, @PathVariable UUID id, @Valid @RequestBody ReverseRequest r) { return service.reverse(tenantId, id, r); }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar pagamentos por ID no contexto da academia")
    public Response get(@PathVariable UUID tenantId, @PathVariable UUID id) { return service.get(tenantId, id); }
}
