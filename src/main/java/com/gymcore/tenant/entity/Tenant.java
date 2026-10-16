package com.gymcore.tenant.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.gymcore.shared.entity.BaseEntity;


@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false)
    private boolean active;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean getActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
