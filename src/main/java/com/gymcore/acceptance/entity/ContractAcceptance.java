package com.gymcore.acceptance.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.gymcore.shared.entity.TenantEntity;
import com.gymcore.contract.entity.Contract;

@Entity
@Table(name = "contract_acceptances")
public class ContractAcceptance extends TenantEntity {
    @Column(nullable = false, updatable = false, unique = true)
    private UUID contractId;
    @Column(nullable = false, length = 40, updatable = false)
    private String termsVersion;
    @Column(nullable = false, columnDefinition = "text", updatable = false)
    private String termsText;
    @Column(nullable = false, length = 120, updatable = false)
    private String acceptedName;
    @Column(nullable = false, length = 120, updatable = false)
    private String recordedBy;
    @Column(nullable = false, updatable = false)
    private Instant occurredAt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "tenantId", referencedColumnName = "tenantId", insertable = false, updatable = false),
        @JoinColumn(name = "contractId", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Contract contract;
    public UUID getContractId() { return contractId; }
    public void setContractId(UUID contractId) { this.contractId = contractId; }
    public String getTermsVersion() { return termsVersion; }
    public void setTermsVersion(String termsVersion) { this.termsVersion = termsVersion; }
    public String getTermsText() { return termsText; }
    public void setTermsText(String termsText) { this.termsText = termsText; }
    public String getAcceptedName() { return acceptedName; }
    public void setAcceptedName(String acceptedName) { this.acceptedName = acceptedName; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public Contract getContract() { return contract; }
}
