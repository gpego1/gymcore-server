package com.gymcore.contract.service;

import java.time.*;
import java.util.UUID;
import com.gymcore.acceptance.entity.ContractAcceptance;
import com.gymcore.acceptance.repository.ContractAcceptanceRepository;
import com.gymcore.contract.dto.ContractDtos.*;
import com.gymcore.contract.entity.Contract;
import com.gymcore.contract.repository.ContractRepository;
import com.gymcore.exception.BusinessException;
import com.gymcore.invoice.entity.Invoice;
import com.gymcore.invoice.enums.InvoiceStatus;
import com.gymcore.invoice.repository.InvoiceRepository;
import com.gymcore.invoice.service.BillingSchedule;
import com.gymcore.plan.service.PlanService;
import com.gymcore.shared.Queries;
import com.gymcore.shared.dto.PageResult;
import com.gymcore.student.service.StudentService;
import com.gymcore.tenant.service.TenantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ContractService {
    private final ContractRepository repository;
    private final ContractAcceptanceRepository acceptances;
    private final InvoiceRepository invoices;
    private final TenantService tenants;
    private final StudentService students;
    private final PlanService plans;
    private final ContractTerms terms;
    private final Clock clock;
    public ContractService(ContractRepository repository, ContractAcceptanceRepository acceptances, InvoiceRepository invoices,
            TenantService tenants, StudentService students, PlanService plans, ContractTerms terms, Clock clock) {
        this.repository = repository; this.acceptances = acceptances; this.invoices = invoices;
        this.tenants = tenants; this.students = students; this.plans = plans; this.terms = terms; this.clock = clock;
    }
    public Contract require(UUID tenantId, UUID id) {
        return repository.findByTenantIdAndId(tenantId, id).orElseThrow(BusinessException::missing);
    }
    private Response response(Contract c) {
        return Response.from(c, acceptances.findByTenantIdAndContractId(c.getTenantId(), c.getId()).orElseThrow());
    }
    @Transactional
    public Response create(UUID tenantId, Request r) {
        tenants.requireActive(tenants.lock(tenantId));
        var student = students.require(tenantId, r.studentId());
        var plan = plans.require(tenantId, r.planId());
        if (!student.getActive() || !plan.getActive()) throw BusinessException.rule("INACTIVE_ENROLLMENT", "Aluno e plano devem estar ativos.");
        if (r.endDate() != null && r.endDate().isBefore(r.startDate())) throw BusinessException.rule("INVALID_TERM", "Término anterior ao início.");
        if (!terms.current().version().equals(r.termsVersion())) throw BusinessException.conflict("TERMS_CHANGED", "Consulte e aceite os termos vigentes.");
        boolean overlaps = repository.findByTenantIdAndStudentIdOrderByStartDate(tenantId, r.studentId()).stream()
                .anyMatch(c -> BillingSchedule.overlaps(r.startDate(), r.endDate(), c.getStartDate(), c.getEndDate()));
        if (overlaps) throw BusinessException.conflict("CONTRACT_OVERLAP", "Já existe contrato com vigência sobreposta.");
        var c = new Contract(); c.setTenantId(tenantId); c.setStudentId(student.getId()); c.setPlanId(plan.getId());
        c.setStartDate(r.startDate()); c.setEndDate(r.endDate()); c.setAmountSnapshot(plan.getMonthlyAmount());
        repository.saveAndFlush(c);
        var a = new ContractAcceptance(); a.setTenantId(tenantId); a.setContractId(c.getId());
        a.setTermsVersion(terms.current().version()); a.setTermsText(terms.current().text());
        a.setAcceptedName(r.acceptedName().trim()); a.setRecordedBy(r.recordedBy().trim()); a.setOccurredAt(clock.instant());
        acceptances.saveAndFlush(a);
        var i = new Invoice(); i.setTenantId(tenantId); i.setContractId(c.getId());
        i.setCompetence(YearMonth.from(c.getStartDate()).atDay(1)); i.setDueDate(c.getStartDate());
        i.setAmount(c.getAmountSnapshot()); i.setStatus(InvoiceStatus.OPEN); invoices.saveAndFlush(i);
        return Response.from(c, a);
    }
    public Response get(UUID tenantId, UUID id) { tenants.require(tenantId); return response(require(tenantId, id)); }
    public PageResult<Response> list(UUID tenantId, UUID studentId, int page, int size) {
        tenants.require(tenantId);
        return PageResult.from(repository.findAll(Queries.<Contract>tenant(tenantId).and(Queries.equal("studentId", studentId)),
            Queries.page(page, size)).map(this::response));
    }
    @Transactional
    public Response cancel(UUID tenantId, UUID id, CancelRequest r) {
        tenants.lock(tenantId); var c = require(tenantId, id);
        if (r.endDate().isBefore(LocalDate.now(clock)) || r.endDate().isBefore(c.getStartDate()))
            throw BusinessException.rule("RETROACTIVE_CANCELLATION", "Término deve ser hoje ou futuro e não anterior ao início.");
        if (c.getCancellationReason() != null) throw BusinessException.conflict("ALREADY_CANCELLED", "Cancelamento já registrado.");
        if (c.getEndDate() != null && r.endDate().isAfter(c.getEndDate()))
            throw BusinessException.rule("INVALID_CANCELLATION", "Cancelamento não pode estender a vigência.");
        c.setEndDate(r.endDate()); c.setCancellationReason(r.reason().trim());
        repository.saveAndFlush(c);
        return response(c);
    }
}
