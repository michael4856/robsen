package com.robsen.robsen.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepo extends JpaRepository<Tenant, String> {

}