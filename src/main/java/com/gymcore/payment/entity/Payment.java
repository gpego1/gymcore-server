package com.gymcore.payment.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.gymcore.shared.entity.TenantEntity;
import com.gymcore.invoice.entity.Invoice;
import com.gymcore.payment.enums.*;

@Entity
@Table(name = "payments")
public class Payment extends TenantEntity {
    @Column(nullable = false, updatable = false)
    private UUID invoiceId;
    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24, updatable = false)
    private PaymentMethod method;
    @Column(length = 200, updatable = false)
    private String reference;
    @Column(nullable = false, length = 120, updatable = false)
    private String recordedBy;
    @Column(nullable = false, updatable = false)
    private Instant confirmedAt;
    @Column
    private Instant reversedAt;
    @Column(length = 120)
    private String reversedBy;
    @Column(length = 500)
    private String reversalReason;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "tenantId", referencedColumnName = "tenantId", insertable = false, updatable = false),
        @JoinColumn(name = "invoiceId", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Invoice invoice;
    public UUID getInvoiceId() { return invoiceId; }
    public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
    public Instant getReversedAt() { return reversedAt; }
    public void setReversedAt(Instant reversedAt) { this.reversedAt = reversedAt; }
    public String getReversedBy() { return reversedBy; }
    public void setReversedBy(String reversedBy) { this.reversedBy = reversedBy; }
    public String getReversalReason() { return reversalReason; }
    public void setReversalReason(String reversalReason) { this.reversalReason = reversalReason; }
    public Invoice getInvoice() { return invoice; }
}
