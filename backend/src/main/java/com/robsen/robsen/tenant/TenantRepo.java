package com.robsen.robsen.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository 
public interface TenantRepo extends JpaRepository<Tenant, Long> {
    java.util.List<Tenant> findByActiveTrue();
}