package com.gymcore.tenant.repository;

import com.gymcore.tenant.entity.Tenant;
import java.util.*;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface TenantRepository extends JpaRepository<Tenant, UUID>, JpaSpecificationExecutor<Tenant> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tenant t where t.id = :id")
    Optional<Tenant> lockById(@Param("id") UUID id);
}
