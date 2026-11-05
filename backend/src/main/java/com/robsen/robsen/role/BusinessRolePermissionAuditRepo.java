package com.robsen.robsen.role;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRolePermissionAuditRepo extends JpaRepository<BusinessRolePermissionAudit, Long> {
}
