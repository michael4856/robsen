package com.robsen.robsen.partnerApplication;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.robsen.robsen.common.ApplicationStatus;

@RestController
@RequestMapping("/api/partner-applications")
public class PartnerApplicationController {
    private final PartnerApplicationService applicationService;

    public PartnerApplicationController(PartnerApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartnerApplication create(@RequestBody PartnerApplication application) {
        return applicationService.create(application);
    }

    @GetMapping("/{id}")
    public PartnerApplication get(@PathVariable Long id) {
        return applicationService.get(id);
    }

    @GetMapping
    public List<PartnerApplication> getAll() {
        return applicationService.getAll();
    }

    @GetMapping("/opportunity/{opportunityId}")
    public List<PartnerApplication> getByOpportunity(@PathVariable Long opportunityId) {
        return applicationService.getByOpportunity(opportunityId);
    }

    @GetMapping("/business/{businessId}")
    public List<PartnerApplication> getByBusiness(@PathVariable Long businessId) {
        return applicationService.getByBusiness(businessId);
    }

    @GetMapping("/status/{status}")
    public List<PartnerApplication> getByStatus(@PathVariable ApplicationStatus status) {
        return applicationService.getByStatus(status);
    }

    @PutMapping("/{id}")
    public PartnerApplication update(@PathVariable Long id, @RequestBody PartnerApplication application) {
        return applicationService.update(id, application);
    }

    @PatchMapping("/{id}/status")
    public PartnerApplication review(@PathVariable Long id, @RequestParam ApplicationStatus status) {
        return applicationService.review(id, status);
    }
}
