package com.gymcore.acceptance.repository;

import com.gymcore.acceptance.entity.ContractAcceptance;
import java.util.*;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ContractAcceptanceRepository extends JpaRepository<ContractAcceptance, UUID>, JpaSpecificationExecutor<ContractAcceptance> {
    Optional<ContractAcceptance> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<ContractAcceptance> findByTenantIdAndContractId(UUID tenantId, UUID contractId);
}
