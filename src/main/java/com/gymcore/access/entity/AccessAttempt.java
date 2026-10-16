package com.gymcore.access.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.gymcore.shared.entity.TenantEntity;
import com.gymcore.student.entity.Student;
import com.gymcore.access.enums.*;

@Entity
@Table(name = "access_attempts")
public class AccessAttempt extends TenantEntity {
    @Column(nullable = false, updatable = false)
    private UUID studentId;
    @Column(nullable = false, length = 120, updatable = false)
    private String recordedBy;
    @Column(nullable = false, updatable = false)
    private Instant occurredAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16, updatable = false)
    private AccessDecision decision;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40, updatable = false)
    private AccessReason reason;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "tenantId", referencedColumnName = "tenantId", insertable = false, updatable = false),
        @JoinColumn(name = "studentId", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Student student;
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public AccessDecision getDecision() { return decision; }
    public void setDecision(AccessDecision decision) { this.decision = decision; }
    public AccessReason getReason() { return reason; }
    public void setReason(AccessReason reason) { this.reason = reason; }
    public Student getStudent() { return student; }
}
