package com.gymcore.tenant.service;

import java.util.UUID;
import com.gymcore.exception.BusinessException;
import com.gymcore.shared.Queries;
import com.gymcore.shared.dto.PageResult;
import com.gymcore.tenant.dto.TenantDtos.*;
import com.gymcore.tenant.entity.Tenant;
import com.gymcore.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TenantService {
    private final TenantRepository repository;
    public TenantService(TenantRepository repository) { this.repository = repository; }

    public Tenant require(UUID id) { return repository.findById(id).orElseThrow(BusinessException::missing); }

    // All mutations acquire this lock first. Serializing per academy keeps the
    // initial monolith simple and makes cross-aggregate financial decisions atomic.
    @Transactional
    public Tenant lock(UUID id) { return repository.lockById(id).orElseThrow(BusinessException::missing); }

    public void requireActive(Tenant tenant) {
        if (!tenant.getActive()) throw BusinessException.rule("TENANT_INACTIVE", "Academia inativa.");
    }
    @Transactional
    public Response create(Request request) {
        var t = new Tenant();
        t.setName(request.name().trim());
        t.setActive(true);
        return Response.from(repository.saveAndFlush(t));
    }
    public PageResult<Response> list(String name, Boolean active, int page, int size) {
        return PageResult.from(repository.findAll(Queries.<Tenant>contains("name", name)
                .and(Queries.equal("active", active)), Queries.page(page, size)).map(Response::from));
    }
    public Response get(UUID id) { return Response.from(require(id)); }
    @Transactional
    public Response update(UUID id, Request request) {
        var t = lock(id);
        t.setName(request.name().trim());
        return Response.from(repository.saveAndFlush(t));
    }
    @Transactional
    public void deactivate(UUID id) { lock(id).setActive(false); }
}
