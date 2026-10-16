package com.gymcore.contract.controller;

import java.util.UUID;
import com.gymcore.contract.service.ContractTerms;
import com.gymcore.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academias/{tenantId}/termos-contratuais/vigentes")
@Tag(name = "contratos")
public class ContractTermsController {
    private final ContractTerms terms;
    private final TenantService tenants;
    public ContractTermsController(ContractTerms terms, TenantService tenants) { this.terms = terms; this.tenants = tenants; }
    @GetMapping
    @Operation(summary = "Consultar versão e conteúdo dos termos vigentes", description = "O contrato usa o conteúdo do servidor; o cliente informa apenas a versão aceita e os dados do aceite presencial.")
    public ContractTerms.Terms current(@PathVariable UUID tenantId) { tenants.require(tenantId); return terms.current(); }
}
