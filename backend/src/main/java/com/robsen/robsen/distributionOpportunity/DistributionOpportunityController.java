package com.robsen.robsen.distributionOpportunity;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/distribution-opportunities")
public class DistributionOpportunityController {
    private final DistributionOpportunityService opportunityService;

    public DistributionOpportunityController(DistributionOpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DistributionOpportiunity create(@RequestBody DistributionOpportiunity opportunity) {
        return opportunityService.create(opportunity);
    }

    @GetMapping("/{id}")
    public DistributionOpportiunity get(@PathVariable Long id) {
        return opportunityService.get(id);
    }

    @GetMapping
    public List<DistributionOpportiunity> getAll() {
        return opportunityService.getAll();
    }

    @GetMapping("/business/{businessId}")
    public List<DistributionOpportiunity> getByBusiness(@PathVariable Long businessId) {
        return opportunityService.getByBusiness(businessId);
    }

    @GetMapping("/territory/{territoryId}")
    public List<DistributionOpportiunity> getByTerritory(@PathVariable Long territoryId) {
        return opportunityService.getByTerritory(territoryId);
    }

    @PutMapping("/{id}")
    public DistributionOpportiunity update(@PathVariable Long id, @RequestBody DistributionOpportiunity opportunity) {
        return opportunityService.update(id, opportunity);
    }

    @DeleteMapping("/{id}")
    public DistributionOpportiunity close(@PathVariable Long id) {
        return opportunityService.close(id);
    }
}
