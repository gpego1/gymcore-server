package com.gymcore.access.service;

import java.time.*;
import java.util.*;
import com.gymcore.access.dto.AccessAttemptDtos.*;
import com.gymcore.access.entity.AccessAttempt;
import com.gymcore.access.enums.*;
import com.gymcore.access.repository.AccessAttemptRepository;
import com.gymcore.contract.repository.ContractRepository;
import com.gymcore.exception.BusinessException;
import com.gymcore.invoice.enums.InvoiceStatus;
import com.gymcore.invoice.repository.InvoiceRepository;
import com.gymcore.invoice.service.BillingSchedule;
import com.gymcore.shared.Queries;
import com.gymcore.shared.dto.PageResult;
import com.gymcore.student.service.StudentService;
import com.gymcore.tenant.service.TenantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccessService {
    private final AccessAttemptRepository repository;
    private final TenantService tenants;
    private final StudentService students;
    private final ContractRepository contracts;
    private final InvoiceRepository invoices;
    private final Clock clock;
    public AccessService(AccessAttemptRepository repository, TenantService tenants, StudentService students,
            ContractRepository contracts, InvoiceRepository invoices, Clock clock) {
        this.repository = repository; this.tenants = tenants; this.students = students;
        this.contracts = contracts; this.invoices = invoices; this.clock = clock;
    }
    @Transactional
    public Response register(UUID tenantId, Request r) {
        var tenant = tenants.lock(tenantId);
        var student = students.require(tenantId, r.studentId());
        var today = LocalDate.now(clock);
        var studentContracts = contracts.findByTenantIdAndStudentIdOrderByStartDate(tenantId, student.getId());
        AccessReason reason;
        if (!tenant.getActive()) reason = AccessReason.TENANT_INACTIVE;
        else if (!student.getActive()) reason = AccessReason.STUDENT_INACTIVE;
        else if (studentContracts.stream().noneMatch(c -> !c.getStartDate().isAfter(today) && (c.getEndDate() == null || !c.getEndDate().isBefore(today))))
            reason = AccessReason.NO_CURRENT_CONTRACT;
        else {
            boolean missing = false;
            boolean overdue = false;
            for (var contract : studentContracts) {
                var existing = invoices.findByTenantIdAndContractId(tenantId, contract.getId());
                var competences = new HashSet<LocalDate>();
                existing.forEach(i -> competences.add(i.getCompetence()));
                if (BillingSchedule.dueDates(contract.getStartDate(), contract.getEndDate(), today)
                        .anyMatch(due -> !competences.contains(YearMonth.from(due).atDay(1)))) missing = true;
                if (existing.stream().anyMatch(i -> i.getStatus() == InvoiceStatus.OPEN && i.getDueDate().isBefore(today))) overdue = true;
            }
            reason = missing ? AccessReason.BILLING_SYNC_PENDING : overdue ? AccessReason.OVERDUE_INVOICE : AccessReason.ELIGIBLE;
        }
        var a = new AccessAttempt(); a.setTenantId(tenantId); a.setStudentId(student.getId()); a.setRecordedBy(r.recordedBy().trim());
        a.setOccurredAt(clock.instant()); a.setReason(reason);
        a.setDecision(reason == AccessReason.ELIGIBLE ? AccessDecision.ALLOWED : reason == AccessReason.BILLING_SYNC_PENDING ? AccessDecision.UNAVAILABLE : AccessDecision.DENIED);
        return Response.from(repository.saveAndFlush(a));
    }
    public Response get(UUID tenantId, UUID id) {
        tenants.require(tenantId);
        return Response.from(repository.findByTenantIdAndId(tenantId, id).orElseThrow(BusinessException::missing));
    }
    public PageResult<Response> list(UUID tenantId, UUID studentId, AccessDecision decision, int page, int size) {
        tenants.require(tenantId);
        return PageResult.from(repository.findAll(Queries.<AccessAttempt>tenant(tenantId).and(Queries.equal("studentId", studentId))
            .and(Queries.equal("decision", decision)), Queries.page(page, size)).map(Response::from));
    }
}
