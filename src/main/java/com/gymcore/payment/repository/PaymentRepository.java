package com.gymcore.payment.repository;

import com.gymcore.payment.entity.Payment;
import java.util.*;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {
    Optional<Payment> findByTenantIdAndId(UUID tenantId, UUID id);
    boolean existsByTenantIdAndInvoiceIdAndReversedAtIsNull(UUID tenantId, UUID invoiceId);
}
