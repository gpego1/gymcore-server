package com.gymcore.invoice.service;

import java.time.*;
import java.util.UUID;
import com.gymcore.contract.repository.ContractRepository;
import com.gymcore.exception.BusinessException;
import com.gymcore.invoice.dto.InvoiceDtos.*;
import com.gymcore.invoice.entity.Invoice;
import com.gymcore.invoice.enums.InvoiceStatus;
import com.gymcore.invoice.repository.InvoiceRepository;
import com.gymcore.payment.repository.PaymentRepository;
import com.gymcore.shared.Queries;
import com.gymcore.shared.dto.PageResult;
import com.gymcore.tenant.service.TenantService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InvoiceService {
    private final InvoiceRepository repository;
    private final ContractRepository contracts;
    private final PaymentRepository payments;
    private final TenantService tenants;
    private final Clock clock;
    public InvoiceService(InvoiceRepository repository, ContractRepository contracts, PaymentRepository payments, TenantService tenants, Clock clock) {
        this.repository = repository; this.contracts = contracts; this.payments = payments; this.tenants = tenants; this.clock = clock;
    }
    public Invoice require(UUID tenantId, UUID id) {
        return repository.findByTenantIdAndId(tenantId, id).orElseThrow(BusinessException::missing);
    }
    @Transactional
    public GenerationResult generate(UUID tenantId, GenerateRequest r) {
        tenants.lock(tenantId);
        if (r.throughDate().isAfter(LocalDate.now(clock))) throw BusinessException.rule("FUTURE_BILLING", "Geração limitada à data comercial atual.");
        int count = 0;
        for (var c : contracts.findByTenantIdOrderById(tenantId)) {
            for (var due : BillingSchedule.dueDates(c.getStartDate(), c.getEndDate(), r.throughDate()).toList()) {
                var competence = YearMonth.from(due).atDay(1);
                if (!repository.existsByTenantIdAndContractIdAndCompetence(tenantId, c.getId(), competence)) {
                    var i = new Invoice(); i.setTenantId(tenantId); i.setContractId(c.getId());
                    i.setCompetence(competence); i.setDueDate(due); i.setAmount(c.getAmountSnapshot()); i.setStatus(InvoiceStatus.OPEN);
                    repository.save(i); count++;
                }
            }
        }
        repository.flush();
        return new GenerationResult(count);
    }
    public Response get(UUID tenantId, UUID id) { tenants.require(tenantId); return Response.from(require(tenantId, id), LocalDate.now(clock)); }
    public PageResult<Response> list(UUID tenantId, UUID studentId, UUID contractId, InvoiceStatus status, Boolean overdue, int page, int size) {
        tenants.require(tenantId);
        Specification<Invoice> spec = Queries.<Invoice>tenant(tenantId).and(Queries.equal("contractId", contractId)).and(Queries.equal("status", status));
        if (studentId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("contract").get("studentId"), studentId));
        var today = LocalDate.now(clock);
        if (overdue != null) spec = spec.and((root, query, cb) -> {
            var isOverdue = cb.and(cb.equal(root.get("status"), InvoiceStatus.OPEN), cb.lessThan(root.get("dueDate"), today));
            return overdue ? isOverdue : cb.not(isOverdue);
        });
        return PageResult.from(repository.findAll(spec, Queries.page(page, size)).map(i -> Response.from(i, today)));
    }
    @Transactional
    public Response cancel(UUID tenantId, UUID id, VoidRequest r) {
        tenants.lock(tenantId); var i = require(tenantId, id);
        if (i.getStatus() != InvoiceStatus.OPEN || payments.existsByTenantIdAndInvoiceIdAndReversedAtIsNull(tenantId, id))
            throw BusinessException.conflict("INVOICE_NOT_VOIDABLE", "Somente cobrança aberta sem pagamento vigente pode ser cancelada.");
        i.setStatus(InvoiceStatus.VOID); i.setVoidReason(r.reason().trim());
        return Response.from(repository.saveAndFlush(i), LocalDate.now(clock));
    }
}
