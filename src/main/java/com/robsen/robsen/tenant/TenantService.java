package com.robsen.robsen.tenant;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

    private final TenantRepo tenantRepo;

    public TenantService(TenantRepo tenantRepo) {
        this.tenantRepo = tenantRepo;
    }

    @Transactional
    public Tenant createTenant(Tenant tenant) {
        return tenantRepo.save(tenant);
    }

    @Transactional(readOnly = true)
    public List<Tenant> getAllTenants() {
        return tenantRepo.findAll();
    }

    @Transactional(readOnly = true)
    public Tenant getTenantById(String tenantId) {
        return tenantRepo.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }
}