package com.robsen.robsen.tenant;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Tenant createTenant(@RequestBody Tenant tenant) {
        return tenantService.createTenant(tenant);
    }

    @GetMapping
    public List<Tenant> getAllTenants() {
        return tenantService.getAllTenants();
    }

    @GetMapping("/active")
    public List<Tenant> getActiveTenants() {
        return tenantService.getActiveTenants();
    }

    @GetMapping("/{tenantId}")
    public Tenant getTenant(@PathVariable Long tenantId) {
        return tenantService.getTenantById(tenantId);
    }

    @PutMapping("/{tenantId}")
    public Tenant updateTenant(@PathVariable Long tenantId, @RequestBody Tenant tenant) {
        return tenantService.updateTenant(tenantId, tenant);
    }

    @DeleteMapping("/{tenantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateTenant(@PathVariable Long tenantId) {
        tenantService.deactivateTenant(tenantId);
    }
}