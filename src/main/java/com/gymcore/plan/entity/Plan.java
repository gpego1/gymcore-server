package com.gymcore.plan.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.gymcore.shared.entity.TenantEntity;


@Entity
@Table(name = "plans")
public class Plan extends TenantEntity {
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyAmount;
    @Column(nullable = false)
    private boolean active;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getMonthlyAmount() { return monthlyAmount; }
    public void setMonthlyAmount(BigDecimal monthlyAmount) { this.monthlyAmount = monthlyAmount; }
    public boolean getActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
