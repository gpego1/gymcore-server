package com.gymcore.contract.repository;

import com.gymcore.contract.entity.Contract;
import java.util.*;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ContractRepository extends JpaRepository<Contract, UUID>, JpaSpecificationExecutor<Contract> {
    Optional<Contract> findByTenantIdAndId(UUID tenantId, UUID id);
    List<Contract> findByTenantIdAndStudentIdOrderByStartDate(UUID tenantId, UUID studentId);
    List<Contract> findByTenantIdOrderById(UUID tenantId);
}
