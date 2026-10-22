package com.robsen.robsen.distributionOpportunity;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DistributionOpportunityService {
    private final DistributionOpportunityRepo opportunityRepo;

    public DistributionOpportunityService(DistributionOpportunityRepo opportunityRepo) {
        this.opportunityRepo = opportunityRepo;
    }

    @Transactional
    public DistributionOpportiunity create(DistributionOpportiunity opportunity) {
        opportunity.setId(null);
        if (opportunity.getStatus() == null || opportunity.getStatus().isBlank()) {
            opportunity.setStatus("OPEN");
        }
        return opportunityRepo.save(opportunity);
    }

    @Transactional(readOnly = true)
    public DistributionOpportiunity get(Long id) {
        return opportunityRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opportunity not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<DistributionOpportiunity> getAll() {
        return opportunityRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<DistributionOpportiunity> getByBusiness(Long businessId) {
        return opportunityRepo.findByBusinessId(businessId);
    }

    @Transactional(readOnly = true)
    public List<DistributionOpportiunity> getByTerritory(Long territoryId) {
        return opportunityRepo.findByTerritoryId(territoryId);
    }

    @Transactional
    public DistributionOpportiunity update(Long id, DistributionOpportiunity input) {
        DistributionOpportiunity opportunity = get(id);
        opportunity.setBusiness(input.getBusiness());
        opportunity.setTitle(input.getTitle());
        opportunity.setDescription(input.getDescription());
        opportunity.setTerritory(input.getTerritory());
        opportunity.setRequirements(input.getRequirements());
        opportunity.setStatus(input.getStatus());
        opportunity.setExpiresAt(input.getExpiresAt());
        return opportunityRepo.save(opportunity);
    }

    @Transactional
    public DistributionOpportiunity close(Long id) {
        DistributionOpportiunity opportunity = get(id);
        opportunity.setStatus("CLOSED");
        return opportunityRepo.save(opportunity);
    }
}
