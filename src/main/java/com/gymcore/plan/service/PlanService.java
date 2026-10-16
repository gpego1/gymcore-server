package com.gymcore.plan.service;

import java.util.UUID;
import com.gymcore.exception.BusinessException;
import com.gymcore.shared.Queries;
import com.gymcore.shared.dto.PageResult;
import com.gymcore.plan.dto.PlanDtos.*;
import com.gymcore.plan.entity.Plan;
import com.gymcore.plan.repository.PlanRepository;
import com.gymcore.tenant.service.TenantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlanService {
    private final PlanRepository repository;
    private final TenantService tenants;
    public PlanService(PlanRepository repository, TenantService tenants) { this.repository = repository; this.tenants = tenants; }
    public Plan require(UUID tenantId, UUID id) {
        return repository.findByTenantIdAndId(tenantId, id).orElseThrow(BusinessException::missing);
    }
    private void apply(Plan p, Request r) { p.setName(r.name().trim()); p.setMonthlyAmount(r.monthlyAmount()); }
    @Transactional
    public Response create(UUID tenantId, Request r) {
        tenants.requireActive(tenants.lock(tenantId));
        var p = new Plan(); p.setTenantId(tenantId); p.setActive(true); apply(p, r);
        return Response.from(repository.saveAndFlush(p));
    }
    @Transactional
    public Response update(UUID tenantId, UUID id, Request r) {
        tenants.lock(tenantId); var p = require(tenantId, id); apply(p, r);
        return Response.from(repository.saveAndFlush(p));
    }
    public Response get(UUID tenantId, UUID id) { tenants.require(tenantId); return Response.from(require(tenantId, id)); }
    public PageResult<Response> list(UUID tenantId, String name, Boolean active, int page, int size) {
        tenants.require(tenantId);
        return PageResult.from(repository.findAll(Queries.<Plan>tenant(tenantId).and(Queries.contains("name", name))
            .and(Queries.equal("active", active)), Queries.page(page, size)).map(Response::from));
    }
    @Transactional
    public void deactivate(UUID tenantId, UUID id) { tenants.lock(tenantId); require(tenantId, id).setActive(false); }
}
