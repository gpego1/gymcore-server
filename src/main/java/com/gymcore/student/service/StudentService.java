package com.gymcore.student.service;

import java.time.*;
import java.util.*;
import com.gymcore.exception.BusinessException;
import com.gymcore.shared.Queries;
import com.gymcore.shared.dto.PageResult;
import com.gymcore.student.dto.StudentDtos.*;
import com.gymcore.student.entity.Student;
import com.gymcore.student.repository.StudentRepository;
import com.gymcore.tenant.service.TenantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentService {
    private final StudentRepository repository;
    private final TenantService tenants;
    private final Clock clock;
    public StudentService(StudentRepository repository, TenantService tenants, Clock clock) {
        this.repository = repository; this.tenants = tenants; this.clock = clock;
    }
    public Student require(UUID tenantId, UUID id) {
        return repository.findByTenantIdAndId(tenantId, id).orElseThrow(BusinessException::missing);
    }
    private void apply(Student s, Request r) {
        if (r.birthDate().isAfter(LocalDate.now(clock).minusYears(18))) {
            throw BusinessException.rule("ADULT_REQUIRED", "O piloto aceita apenas alunos com 18 anos completos.");
        }
        var email = r.email().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByTenantIdAndEmailAndIdNot(s.getTenantId(), email, s.getId())) {
            throw BusinessException.conflict("DUPLICATE_EMAIL", "E-mail já cadastrado nesta academia.");
        }
        s.setName(r.name().trim()); s.setEmail(email); s.setPhone(r.phone()); s.setBirthDate(r.birthDate());
    }
    @Transactional
    public Response create(UUID tenantId, Request r) {
        tenants.requireActive(tenants.lock(tenantId));
        var s = new Student(); s.setTenantId(tenantId); s.setActive(true);
        apply(s, r);
        return Response.from(repository.saveAndFlush(s));
    }
    @Transactional
    public Response update(UUID tenantId, UUID id, Request r) {
        tenants.lock(tenantId);
        var s = require(tenantId, id); apply(s, r);
        return Response.from(repository.saveAndFlush(s));
    }
    public Response get(UUID tenantId, UUID id) { tenants.require(tenantId); return Response.from(require(tenantId, id)); }
    public PageResult<Response> list(UUID tenantId, String name, Boolean active, int page, int size) {
        tenants.require(tenantId);
        return PageResult.from(repository.findAll(Queries.<Student>tenant(tenantId)
            .and(Queries.contains("name", name)).and(Queries.equal("active", active)), Queries.page(page, size)).map(Response::from));
    }
    @Transactional
    public void deactivate(UUID tenantId, UUID id) { tenants.lock(tenantId); require(tenantId, id).setActive(false); }
}
