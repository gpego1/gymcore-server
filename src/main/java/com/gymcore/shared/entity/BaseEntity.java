package com.gymcore.shared.entity;

import jakarta.persistence.*;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {
    @Id
    protected UUID id = UUID.randomUUID();
    @Version
    protected Long version;

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
}
