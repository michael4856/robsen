package com.robsen.robsen.role;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRolePermissionConfigurationRepo
        extends JpaRepository<BusinessRolePermissionConfiguration, Long> {
    Optional<BusinessRolePermissionConfiguration> findByBusinessIdAndRoleId(Long businessId, Long roleId);
}
