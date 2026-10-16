package com.gymcore.access.repository;

import com.gymcore.access.entity.AccessAttempt;
import java.util.*;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AccessAttemptRepository extends JpaRepository<AccessAttempt, UUID>, JpaSpecificationExecutor<AccessAttempt> {
    Optional<AccessAttempt> findByTenantIdAndId(UUID tenantId, UUID id);
}
