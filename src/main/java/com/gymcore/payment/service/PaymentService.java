package com.gymcore.payment.service;

import java.time.Clock;
import java.util.UUID;
import com.gymcore.exception.BusinessException;
import com.gymcore.invoice.enums.InvoiceStatus;
import com.gymcore.invoice.service.InvoiceService;
import com.gymcore.payment.dto.PaymentDtos.*;
import com.gymcore.payment.entity.Payment;
import com.gymcore.payment.repository.PaymentRepository;
import com.gymcore.shared.Queries;
import com.gymcore.shared.dto.PageResult;
import com.gymcore.tenant.service.TenantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PaymentService {
    private final PaymentRepository repository;
    private final InvoiceService invoices;
    private final TenantService tenants;
    private final Clock clock;
    public PaymentService(PaymentRepository repository, InvoiceService invoices, TenantService tenants, Clock clock) {
        this.repository = repository; this.invoices = invoices; this.tenants = tenants; this.clock = clock;
    }
    public Payment require(UUID tenantId, UUID id) {
        return repository.findByTenantIdAndId(tenantId, id).orElseThrow(BusinessException::missing);
    }
    @Transactional
    public Response create(UUID tenantId, Request r) {
        tenants.lock(tenantId); var invoice = invoices.require(tenantId, r.invoiceId());
        if (invoice.getStatus() != InvoiceStatus.OPEN || repository.existsByTenantIdAndInvoiceIdAndReversedAtIsNull(tenantId, invoice.getId()))
            throw BusinessException.conflict("INVOICE_NOT_PAYABLE", "Cobrança já paga ou cancelada.");
        if (r.amount().compareTo(invoice.getAmount()) != 0)
            throw BusinessException.rule("FULL_PAYMENT_REQUIRED", "O valor deve corresponder ao saldo integral da cobrança.");
        var p = new Payment(); p.setTenantId(tenantId); p.setInvoiceId(invoice.getId()); p.setAmount(r.amount());
        p.setMethod(r.method()); p.setReference(r.reference()); p.setRecordedBy(r.recordedBy().trim()); p.setConfirmedAt(clock.instant());
        invoice.setStatus(InvoiceStatus.PAID);
        return Response.from(repository.saveAndFlush(p));
    }
    @Transactional
    public Response reverse(UUID tenantId, UUID id, ReverseRequest r) {
        tenants.lock(tenantId); var p = require(tenantId, id);
        if (p.getReversedAt() != null) throw BusinessException.conflict("PAYMENT_REVERSED", "Pagamento já estornado.");
        var invoice = invoices.require(tenantId, p.getInvoiceId());
        if (invoice.getStatus() != InvoiceStatus.PAID) throw BusinessException.conflict("INCONSISTENT_PAYMENT", "Situação da cobrança incompatível com estorno.");
        p.setReversedAt(clock.instant()); p.setReversedBy(r.recordedBy().trim()); p.setReversalReason(r.reason().trim());
        invoice.setStatus(InvoiceStatus.OPEN);
        return Response.from(repository.saveAndFlush(p));
    }
    public Response get(UUID tenantId, UUID id) { tenants.require(tenantId); return Response.from(require(tenantId, id)); }
    public PageResult<Response> list(UUID tenantId, UUID invoiceId, Boolean reversed, int page, int size) {
        tenants.require(tenantId);
        var spec = Queries.<Payment>tenant(tenantId).and(Queries.equal("invoiceId", invoiceId));
        if (reversed != null) spec = spec.and((root, query, cb) -> reversed ? cb.isNotNull(root.get("reversedAt")) : cb.isNull(root.get("reversedAt")));
        return PageResult.from(repository.findAll(spec, Queries.page(page, size)).map(Response::from));
    }
}
