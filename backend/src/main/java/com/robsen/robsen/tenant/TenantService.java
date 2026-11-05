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
        tenant.setTenantId(null);
        tenant.setActive(true);
        return tenantRepo.save(tenant);
    }

    @Transactional(readOnly = true)
    public List<Tenant> getAllTenants() {
        return tenantRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<Tenant> getActiveTenants() {
        return tenantRepo.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Tenant getTenantById(Long tenantId) {
        return tenantRepo.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    @Transactional
    public Tenant updateTenant(Long tenantId, Tenant input) {
        Tenant tenant = getTenantById(tenantId);
        tenant.setTenantName(input.getTenantName());
        tenant.setDescription(input.getDescription());
        tenant.setLocation(input.getLocation());
        tenant.setActive(input.isActive());
        return tenantRepo.save(tenant);
    }

    @Transactional
    public void deactivateTenant(Long tenantId) {
        Tenant tenant = getTenantById(tenantId);
        tenant.setActive(false);
        tenantRepo.save(tenant);
    }
}