package com.gymcore.shared.entity;

import com.gymcore.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.util.UUID;

@MappedSuperclass
public abstract class TenantEntity extends BaseEntity {
    @Column(nullable = false, updatable = false)
    protected UUID tenantId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenantId", insertable = false, updatable = false)
    protected Tenant tenant;

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
}
