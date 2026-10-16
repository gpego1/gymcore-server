package com.gymcore.invoice.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.gymcore.shared.entity.TenantEntity;
import com.gymcore.contract.entity.Contract;
import com.gymcore.invoice.enums.*;

@Entity
@Table(name = "invoices")
public class Invoice extends TenantEntity {
    @Column(nullable = false, updatable = false)
    private UUID contractId;
    @Column(nullable = false, updatable = false)
    private LocalDate competence;
    @Column(nullable = false, updatable = false)
    private LocalDate dueDate;
    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private InvoiceStatus status;
    @Column(length = 500)
    private String voidReason;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "tenantId", referencedColumnName = "tenantId", insertable = false, updatable = false),
        @JoinColumn(name = "contractId", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Contract contract;
    public UUID getContractId() { return contractId; }
    public void setContractId(UUID contractId) { this.contractId = contractId; }
    public LocalDate getCompetence() { return competence; }
    public void setCompetence(LocalDate competence) { this.competence = competence; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }
    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }
    public Contract getContract() { return contract; }
}
