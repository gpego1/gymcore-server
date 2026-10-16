package com.gymcore.invoice.repository;

import com.gymcore.invoice.entity.Invoice;
import java.util.*;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {
    Optional<Invoice> findByTenantIdAndId(UUID tenantId, UUID id);
    boolean existsByTenantIdAndContractIdAndCompetence(UUID tenantId, UUID contractId, LocalDate competence);
    List<Invoice> findByTenantIdAndContractId(UUID tenantId, UUID contractId);
}
