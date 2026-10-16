package com.gymcore.student.repository;

import com.gymcore.student.entity.Student;
import java.util.*;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, UUID>, JpaSpecificationExecutor<Student> {
    Optional<Student> findByTenantIdAndId(UUID tenantId, UUID id);
    boolean existsByTenantIdAndEmailAndIdNot(UUID tenantId, String email, UUID id);
}
