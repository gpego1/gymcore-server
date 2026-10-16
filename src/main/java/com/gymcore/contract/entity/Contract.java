package com.gymcore.contract.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.gymcore.shared.entity.TenantEntity;
import com.gymcore.student.entity.Student;
import com.gymcore.plan.entity.Plan;

@Entity
@Table(name = "contracts")
public class Contract extends TenantEntity {
    @Column(nullable = false, updatable = false)
    private UUID studentId;
    @Column(nullable = false, updatable = false)
    private UUID planId;
    @Column(nullable = false, updatable = false)
    private LocalDate startDate;
    @Column
    private LocalDate endDate;
    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amountSnapshot;
    @Column(length = 500)
    private String cancellationReason;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "tenantId", referencedColumnName = "tenantId", insertable = false, updatable = false),
        @JoinColumn(name = "studentId", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Student student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "tenantId", referencedColumnName = "tenantId", insertable = false, updatable = false),
        @JoinColumn(name = "planId", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Plan plan;
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID planId) { this.planId = planId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BigDecimal getAmountSnapshot() { return amountSnapshot; }
    public void setAmountSnapshot(BigDecimal amountSnapshot) { this.amountSnapshot = amountSnapshot; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public Student getStudent() { return student; }
    public Plan getPlan() { return plan; }
}
