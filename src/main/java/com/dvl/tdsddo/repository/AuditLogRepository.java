package com.dvl.tdsddo.repository;

import com.dvl.tdsddo.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog,Long> {
}
