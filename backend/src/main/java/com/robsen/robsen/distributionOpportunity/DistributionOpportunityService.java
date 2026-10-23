package com.robsen.robsen.distributionOpportunity;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.robsen.robsen.security.BusinessAccessService;
import com.robsen.robsen.territory.Territory;
import com.robsen.robsen.territory.TerritoryRepo;

@Service
public class DistributionOpportunityService {
    private final DistributionOpportunityRepo opportunityRepo;
    private final BusinessAccessService businessAccess;
    private final TerritoryRepo territoryRepo;

    public DistributionOpportunityService(
            DistributionOpportunityRepo opportunityRepo,
            BusinessAccessService businessAccess,
            TerritoryRepo territoryRepo) {
        this.opportunityRepo = opportunityRepo;
        this.businessAccess = businessAccess;
        this.territoryRepo = territoryRepo;
    }

    @Transactional
    public DistributionOpportiunity create(DistributionOpportiunity opportunity) {
        opportunity.setId(null);
        businessAccess.requireAccess(opportunity.getBusiness().getId(), "opportunities:CREATE");
        validateTerritory(opportunity.getTerritory(), opportunity.getBusiness().getId());
        if (opportunity.getStatus() == null || opportunity.getStatus().isBlank()) {
            opportunity.setStatus("OPEN");
        }
        return opportunityRepo.save(opportunity);
    }

    @Transactional(readOnly = true)
    public DistributionOpportiunity get(Long id) {
        DistributionOpportiunity opportunity = opportunityRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opportunity not found: " + id));
        businessAccess.requireAccess(opportunity.getBusiness().getId(), "opportunities:READ");
        return opportunity;
    }

    @Transactional(readOnly = true)
    public List<DistributionOpportiunity> getAll() {
        if (businessAccess.isSuperAdmin()) return opportunityRepo.findAll();
        List<Long> businessIds = businessAccess.accessibleBusinessIds();
        if (businessIds.isEmpty()) return List.of();
        return opportunityRepo.findByBusinessIdIn(businessIds).stream()
                .filter(opportunity -> businessAccess.canAccess(
                        opportunity.getBusiness().getId(), "opportunities:READ"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DistributionOpportiunity> getByBusiness(Long businessId) {
        businessAccess.requireAccess(businessId, "opportunities:READ");
        return opportunityRepo.findByBusinessId(businessId);
    }

    @Transactional(readOnly = true)
    public List<DistributionOpportiunity> getByTerritory(Long territoryId) {
        return getAll().stream()
                .filter(opportunity -> opportunity.getTerritory() != null
                        && opportunity.getTerritory().getId().equals(territoryId))
                .toList();
    }

    @Transactional
    public DistributionOpportiunity update(Long id, DistributionOpportiunity input) {
        DistributionOpportiunity opportunity = get(id);
        businessAccess.requireAccess(opportunity.getBusiness().getId(), "opportunities:DELETE");
        businessAccess.requireAccess(input.getBusiness().getId(), "opportunities:UPDATE");
        validateTerritory(input.getTerritory(), input.getBusiness().getId());
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
        businessAccess.requireAccess(opportunity.getBusiness().getId(), "opportunities:UPDATE");
        opportunity.setStatus("CLOSED");
        return opportunityRepo.save(opportunity);
    }

    private void validateTerritory(Territory territory, Long businessId) {
        if (territory == null) return;
        Territory existingTerritory = territoryRepo.findById(territory.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Territory not found."));
        if (!businessAccess.isSuperAdmin()
                && (existingTerritory.getBusiness() == null
                        || !businessId.equals(existingTerritory.getBusiness().getId()))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "The territory must belong to the opportunity's business.");
        }
    }
}
