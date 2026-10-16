package com.gymcore.plan.repository;

import com.gymcore.plan.entity.Plan;
import java.util.*;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PlanRepository extends JpaRepository<Plan, UUID>, JpaSpecificationExecutor<Plan> {
    Optional<Plan> findByTenantIdAndId(UUID tenantId, UUID id);
}
