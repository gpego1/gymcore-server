package com.gymcore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymcore.acceptance.repository.ContractAcceptanceRepository;
import com.gymcore.access.dto.AccessAttemptDtos;
import com.gymcore.access.enums.*;
import com.gymcore.access.service.AccessService;
import com.gymcore.contract.dto.ContractDtos;
import com.gymcore.contract.repository.ContractRepository;
import com.gymcore.contract.service.ContractService;
import com.gymcore.exception.BusinessException;
import com.gymcore.invoice.dto.InvoiceDtos;
import com.gymcore.invoice.entity.Invoice;
import com.gymcore.invoice.enums.InvoiceStatus;
import com.gymcore.invoice.repository.InvoiceRepository;
import com.gymcore.invoice.service.InvoiceService;
import com.gymcore.payment.dto.PaymentDtos;
import com.gymcore.payment.enums.PaymentMethod;
import com.gymcore.payment.service.PaymentService;
import com.gymcore.plan.dto.PlanDtos;
import com.gymcore.plan.service.PlanService;
import com.gymcore.shared.Queries;
import com.gymcore.student.dto.StudentDtos;
import com.gymcore.student.service.StudentService;
import com.gymcore.tenant.dto.TenantDtos;
import com.gymcore.tenant.service.TenantService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(CoreIntegrationTest.FixedTime.class)
@EnabledIfEnvironmentVariable(named = "GYMCORE_TEST_DB_URL", matches = ".+")
class CoreIntegrationTest {
    static final LocalDate TODAY = LocalDate.of(2026, 10, 16);
    static final BigDecimal PRICE = new BigDecimal("120.00");
    @TestConfiguration
    static class FixedTime {
        @Bean @Primary Clock testClock() { return Clock.fixed(Instant.parse("2026-10-16T15:00:00Z"), ZoneId.of("America/Sao_Paulo")); }
    }
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> System.getenv("GYMCORE_TEST_DB_URL"));
        r.add("spring.datasource.username", () -> System.getenv().getOrDefault("GYMCORE_TEST_DB_USERNAME", "gymcore_test"));
        r.add("spring.datasource.password", () -> System.getenv().getOrDefault("GYMCORE_TEST_DB_PASSWORD", ""));
    }
    @Autowired TenantService tenants;
    @Autowired StudentService students;
    @Autowired PlanService plans;
    @Autowired ContractService contracts;
    @Autowired InvoiceService invoices;
    @Autowired PaymentService payments;
    @Autowired AccessService access;
    @Autowired ContractRepository contractRepository;
    @Autowired ContractAcceptanceRepository acceptanceRepository;
    @MockitoSpyBean InvoiceRepository invoiceRepository;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    UUID tenantId;
    UUID studentId;
    UUID planId;

    @BeforeEach
    void setup() {
        tenantId = tenants.create(new TenantDtos.Request("Academia Teste")).id();
        studentId = students.create(tenantId, studentRequest("ana@example.com")).id();
        planId = plans.create(tenantId, new PlanDtos.Request("Mensal", PRICE)).id();
    }
    StudentDtos.Request studentRequest(String email) { return new StudentDtos.Request("Ana Teste", email, "+5511999999999", LocalDate.of(1995, 1, 1)); }
    ContractDtos.Request enrollment(LocalDate start, LocalDate end) {
        return new ContractDtos.Request(studentId, planId, start, end, "2026-10-v1", "Ana Teste", "Operador Teste");
    }
    ContractDtos.Response enroll(LocalDate start) { return contracts.create(tenantId, enrollment(start, null)); }
    List<InvoiceDtos.Response> bills() { return invoices.list(tenantId, studentId, null, null, null, 0, 100).content(); }
    PaymentDtos.Response pay(UUID invoiceId) { return payments.create(tenantId, new PaymentDtos.Request(invoiceId, PRICE, PaymentMethod.CASH, null, "Operador")); }
    AccessAttemptDtos.Response checkAccess() { return access.register(tenantId, new AccessAttemptDtos.Request(studentId, "Operador")); }
    void error(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }
    String base() { return "/api/v1/academias/" + tenantId; }

    @Test
    void normalizesEmailAndRejectsDuplicatesOnlyWithinAcademy() {
        error(() -> students.create(tenantId, studentRequest("ANA@EXAMPLE.COM")), "DUPLICATE_EMAIL");
        UUID other = tenants.create(new TenantDtos.Request("Outra")).id();
        assertThat(students.create(other, studentRequest("ANA@EXAMPLE.COM")).email()).isEqualTo("ana@example.com");
        assertThat(students.list(other, null, null, 0, 20).totalElements()).isEqualTo(1);
        error(() -> students.get(other, studentId), "NOT_FOUND");
    }
    @Test
    void rejectsCrossAcademyEnrollmentAtServiceAndDatabase() {
        UUID other = tenants.create(new TenantDtos.Request("Outra")).id();
        error(() -> contracts.create(other, enrollment(TODAY, null)), "NOT_FOUND");
        var otherStudent = students.create(other, studentRequest("other@example.com"));
        assertThatThrownBy(() -> jdbc.update("INSERT INTO contracts (id,tenant_id,version,student_id,plan_id,start_date,amount_snapshot) VALUES (?,?,0,?,?,?,?)",
            UUID.randomUUID(), tenantId, otherStudent.id(), planId, TODAY, PRICE)).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test
    void validatesAdultBoundary() {
        var boundary = new StudentDtos.Request("Adulto", "adult@example.com", "+5511999999999", TODAY.minusYears(18));
        assertThat(students.create(tenantId, boundary).id()).isNotNull();
        error(() -> students.create(tenantId, new StudentDtos.Request("Menor", "minor@example.com", "+5511999999999", TODAY.minusYears(18).plusDays(1))), "ADULT_REQUIRED");
    }
    @Test
    void rejectsInactiveStudentsAndPlans() {
        students.deactivate(tenantId, studentId);
        error(() -> enroll(TODAY), "INACTIVE_ENROLLMENT");
        studentId = students.create(tenantId, studentRequest("new@example.com")).id();
        plans.deactivate(tenantId, planId);
        error(() -> enroll(TODAY), "INACTIVE_ENROLLMENT");
    }
    @Test
    void createsAcceptanceAndFirstBillAndSnapshotsPrice() {
        var c = enroll(TODAY);
        assertThat(c.acceptance().termsText()).contains("sem pró-rata");
        assertThat(c.acceptance().recordedBy()).isEqualTo("Operador Teste");
        assertThat(bills()).hasSize(1);
        assertThat(bills().getFirst().dueDate()).isEqualTo(TODAY);
        plans.update(tenantId, planId, new PlanDtos.Request("Novo", new BigDecimal("999.00")));
        assertThat(contracts.get(tenantId, c.id()).amountSnapshot()).isEqualByComparingTo(PRICE);
        assertThat(bills().getFirst().amount()).isEqualByComparingTo(PRICE);
    }
    @Test
    void rollsBackContractAndAcceptanceWhenFirstInvoiceFails() {
        doThrow(new DataIntegrityViolationException("test failure")).when(invoiceRepository).saveAndFlush(any(Invoice.class));
        assertThatThrownBy(() -> enroll(TODAY)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(contractRepository.count(Queries.tenant(tenantId))).isZero();
        assertThat(acceptanceRepository.count(Queries.tenant(tenantId))).isZero();
    }
    @Test
    void requiresCurrentTermsAndValidDates() {
        var request = new ContractDtos.Request(studentId, planId, TODAY, null, "old", "Ana", "Operador");
        error(() -> contracts.create(tenantId, request), "TERMS_CHANGED");
        error(() -> contracts.create(tenantId, enrollment(TODAY, TODAY.minusDays(1))), "INVALID_TERM");
    }
    @Test
    void rejectsInclusiveOverlapAndAllowsNextDay() {
        contracts.create(tenantId, enrollment(TODAY.minusDays(10), TODAY));
        error(() -> enroll(TODAY), "CONTRACT_OVERLAP");
        assertThat(enroll(TODAY.plusDays(1)).id()).isNotNull();
    }
    @Test
    void generationIsIdempotentAndUsesOriginalBillingDayAndSnapshot() {
        enroll(LocalDate.of(2024, 1, 31));
        plans.update(tenantId, planId, new PlanDtos.Request("Ajustado", new BigDecimal("500.00")));
        assertThat(invoices.generate(tenantId, new InvoiceDtos.GenerateRequest(LocalDate.of(2024, 3, 31))).created()).isEqualTo(2);
        assertThat(invoices.generate(tenantId, new InvoiceDtos.GenerateRequest(LocalDate.of(2024, 3, 31))).created()).isZero();
        assertThat(bills()).extracting(InvoiceDtos.Response::dueDate).containsExactlyInAnyOrder(LocalDate.of(2024, 1, 31), LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 31));
        assertThat(bills()).allSatisfy(i -> assertThat(i.amount()).isEqualByComparingTo(PRICE));
        error(() -> invoices.generate(tenantId, new InvoiceDtos.GenerateRequest(TODAY.plusDays(1))), "FUTURE_BILLING");
    }
    @Test
    void cancellationIsInclusiveAndPreservesDebtWithoutFutureGeneration() {
        var c = enroll(TODAY.minusMonths(2));
        contracts.cancel(tenantId, c.id(), new ContractDtos.CancelRequest(TODAY, "Solicitação"));
        assertThat(invoices.generate(tenantId, new InvoiceDtos.GenerateRequest(TODAY)).created()).isEqualTo(2);
        assertThat(bills()).hasSize(3);
        assertThat(checkAccess().reason()).isEqualTo(AccessReason.OVERDUE_INVOICE);
        assertThat(bills()).allMatch(i -> !i.dueDate().isAfter(TODAY));
        error(() -> contracts.cancel(tenantId, c.id(), new ContractDtos.CancelRequest(TODAY, "Novamente")), "ALREADY_CANCELLED");
    }
    @Test
    void rejectsRetroactiveCancellationAndExtension() {
        var c = contracts.create(tenantId, enrollment(TODAY.minusMonths(1), TODAY.plusDays(5)));
        error(() -> contracts.cancel(tenantId, c.id(), new ContractDtos.CancelRequest(TODAY.minusDays(1), "Passado")), "RETROACTIVE_CANCELLATION");
        error(() -> contracts.cancel(tenantId, c.id(), new ContractDtos.CancelRequest(TODAY.plusDays(6), "Estender")), "INVALID_CANCELLATION");
    }
    @Test
    void fullPaymentsOnlyAndReversalReopensExactlyOneInvoice() {
        enroll(TODAY.minusMonths(2));
        invoices.generate(tenantId, new InvoiceDtos.GenerateRequest(TODAY));
        var old = bills().stream().filter(InvoiceDtos.Response::overdue).toList();
        assertThat(old).hasSize(2);
        error(() -> payments.create(tenantId, new PaymentDtos.Request(old.getFirst().id(), new BigDecimal("1.00"), PaymentMethod.CASH, null, "Op")), "FULL_PAYMENT_REQUIRED");
        var p = pay(old.getFirst().id());
        error(() -> pay(old.getFirst().id()), "INVOICE_NOT_PAYABLE");
        assertThat(checkAccess().reason()).isEqualTo(AccessReason.OVERDUE_INVOICE);
        payments.reverse(tenantId, p.id(), new PaymentDtos.ReverseRequest("Correção", "Operador"));
        assertThat(payments.get(tenantId, p.id()).reversedAt()).isNotNull();
        assertThat(invoices.get(tenantId, p.invoiceId()).status()).isEqualTo(InvoiceStatus.OPEN);
        error(() -> payments.reverse(tenantId, p.id(), new PaymentDtos.ReverseRequest("Outra", "Op")), "PAYMENT_REVERSED");
        assertThat(pay(p.invoiceId()).id()).isNotEqualTo(p.id());
        pay(old.get(1).id());
        assertThat(checkAccess().decision()).isEqualTo(AccessDecision.ALLOWED);
        assertThat(payments.list(tenantId, p.invoiceId(), null, 0, 20).totalElements()).isEqualTo(2);
    }
    @Test
    void voidRequiresNoCurrentPaymentAndRemainsIdempotentlyMaterialized() {
        enroll(TODAY);
        var invoice = bills().getFirst();
        var p = pay(invoice.id());
        error(() -> invoices.cancel(tenantId, invoice.id(), new InvoiceDtos.VoidRequest("Indevida")), "INVOICE_NOT_VOIDABLE");
        payments.reverse(tenantId, p.id(), new PaymentDtos.ReverseRequest("Correção", "Op"));
        invoices.cancel(tenantId, invoice.id(), new InvoiceDtos.VoidRequest("Indevida"));
        error(() -> pay(invoice.id()), "INVOICE_NOT_PAYABLE");
        assertThat(invoices.generate(tenantId, new InvoiceDtos.GenerateRequest(TODAY)).created()).isZero();
        assertThat(invoices.get(tenantId, invoice.id()).status()).isEqualTo(InvoiceStatus.VOID);
    }
    @Test
    void accessRecordsAllDecisionKindsWithoutGeneratingInvoices() throws Exception {
        assertThat(checkAccess().reason()).isEqualTo(AccessReason.NO_CURRENT_CONTRACT);
        enroll(TODAY.minusMonths(1));
        var before = bills().size();
        mvc.perform(post(base()+"/acessos").contentType("application/json")
            .content(json.writeValueAsString(new AccessAttemptDtos.Request(studentId, "Operador"))))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("code").value("BILLING_SYNC_PENDING"))
            .andExpect(jsonPath("accessAttempt.decision").value("UNAVAILABLE"));
        assertThat(bills()).hasSize(before);
        assertThat(access.list(tenantId, studentId, AccessDecision.UNAVAILABLE, 0, 20).totalElements()).isEqualTo(1);
        invoices.generate(tenantId, new InvoiceDtos.GenerateRequest(TODAY));
        assertThat(checkAccess().reason()).isEqualTo(AccessReason.OVERDUE_INVOICE);
        bills().forEach(i -> pay(i.id()));
        assertThat(checkAccess().decision()).isEqualTo(AccessDecision.ALLOWED);
        students.deactivate(tenantId, studentId);
        assertThat(checkAccess().reason()).isEqualTo(AccessReason.STUDENT_INACTIVE);
        assertThat(bills()).hasSize(2);
        tenants.deactivate(tenantId);
        assertThat(checkAccess().reason()).isEqualTo(AccessReason.TENANT_INACTIVE);
        assertThat(payments.list(tenantId, null, null, 0, 20).totalElements()).isEqualTo(2);
    }
    @Test
    void getEndpointsNeverMutateBillingAndFiltersAreScoped() throws Exception {
        enroll(TODAY.minusMonths(1));
        var version = bills().getFirst().version();
        mvc.perform(get(base()+"/mensalidades").param("studentId", studentId.toString()).param("overdue", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1));
        assertThat(bills()).hasSize(1);
        assertThat(bills().getFirst().version()).isEqualTo(version);
        UUID other = tenants.create(new TenantDtos.Request("Outra")).id();
        assertThat(invoices.list(other, studentId, null, null, null, 0, 20).totalElements()).isZero();
    }
    @Test
    void uniqueConstraintsProtectEmailInvoicesAndCurrentPayments() {
        enroll(TODAY);
        var i = bills().getFirst();
        assertThatThrownBy(() -> jdbc.update("INSERT INTO invoices(id,tenant_id,version,contract_id,competence,due_date,amount,status) VALUES (?,?,0,?,?,?,?, 'OPEN')",
            UUID.randomUUID(), tenantId, i.contractId(), i.competence(), i.dueDate(), PRICE)).isInstanceOf(DataIntegrityViolationException.class);
        pay(i.id());
        assertThatThrownBy(() -> jdbc.update("INSERT INTO payments(id,tenant_id,version,invoice_id,amount,method,recorded_by,confirmed_at) VALUES (?,?,0,?,?,'CASH','Op',now())",
            UUID.randomUUID(), tenantId, i.id(), PRICE)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO students(id,tenant_id,version,name,email,phone,birth_date,active) VALUES (?,?,0,'Outro','ana@example.com','+5511999999999','1990-01-01',true)",
            UUID.randomUUID(), tenantId)).isInstanceOf(DataIntegrityViolationException.class);
    }
    List<Boolean> concurrently(Callable<Boolean> action) throws Exception {
        var ready = new CountDownLatch(2);
        var go = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> task = () -> { ready.countDown(); if (!go.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("barrier timeout"); return action.call(); };
            var a = pool.submit(task); var b = pool.submit(task);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); go.countDown();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        }
    }
    @Test
    void concurrentPaymentsAcceptExactlyOne() throws Exception {
        enroll(TODAY); UUID invoiceId = bills().getFirst().id();
        var results = concurrently(() -> { try { pay(invoiceId); return true; } catch (BusinessException e) { assertThat(e.getStatus()).isEqualTo(409); return false; } });
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(payments.list(tenantId, invoiceId, false, 0, 20).totalElements()).isEqualTo(1);
        assertThat(invoices.get(tenantId, invoiceId).status()).isEqualTo(InvoiceStatus.PAID);
    }
    @Test
    void concurrentEnrollmentsCannotOverlap() throws Exception {
        var results = concurrently(() -> { try { enroll(TODAY); return true; } catch (BusinessException e) { assertThat(e.getCode()).isEqualTo("CONTRACT_OVERLAP"); return false; } });
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(bills()).hasSize(1);
    }
    @Test
    void concurrentGenerationDoesNotDuplicate() throws Exception {
        enroll(TODAY.minusMonths(1));
        var results = concurrently(() -> invoices.generate(tenantId, new InvoiceDtos.GenerateRequest(TODAY)).created() == 1);
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(bills()).hasSize(2);
    }
    @Test
    void validatesHttpErrorsAndSwagger() throws Exception {
        mvc.perform(post(base()+"/alunos").contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("fieldErrors.name").exists()).andExpect(jsonPath("instance").value(base()+"/alunos"));
        mvc.perform(get(base()+"/alunos").param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get(base()+"/mensalidades").param("status", "UNKNOWN")).andExpect(status().isBadRequest());
        mvc.perform(post(base()+"/pagamentos").contentType("application/json").content("{\"method\":\"UNKNOWN\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("INVALID_REQUEST"));
        mvc.perform(post(base()+"/alunos").contentType("application/json").content("{"))
            .andExpect(status().isBadRequest());
        mvc.perform(get(base()+"/alunos/"+UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("info.title").value("GymCore — API local"))
            .andExpect(jsonPath("paths").exists());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
