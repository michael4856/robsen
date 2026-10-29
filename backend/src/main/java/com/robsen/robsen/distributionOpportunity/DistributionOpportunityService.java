package com.robsen.robsen.distributionOpportunity;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.robsen.robsen.security.BusinessAccessService;
import com.robsen.robsen.territory.Territory;
import com.robsen.robsen.territory.TerritoryRepo;
import com.robsen.robsen.business.BusinessRepo;
import com.robsen.robsen.business.Business;
import com.robsen.robsen.common.Status;

@Service
public class DistributionOpportunityService {
    private final DistributionOpportunityRepo opportunityRepo;
    private final BusinessAccessService businessAccess;
    private final TerritoryRepo territoryRepo;
    private final BusinessRepo businessRepo;

    public DistributionOpportunityService(
            DistributionOpportunityRepo opportunityRepo,
            BusinessAccessService businessAccess,
            TerritoryRepo territoryRepo,
            BusinessRepo businessRepo) {
        this.opportunityRepo = opportunityRepo;
        this.businessAccess = businessAccess;
        this.territoryRepo = territoryRepo;
        this.businessRepo = businessRepo;
    }

    @Transactional
    public DistributionOpportiunity create(DistributionOpportiunity opportunity) {
        opportunity.setId(null);
        businessAccess.requireAccess(opportunity.getBusiness().getId(), "opportunities:CREATE");
        validateTerritory(opportunity.getTerritory(), opportunity.getBusiness().getId());
        if (opportunity.getStatus() == null || opportunity.getStatus().isBlank()) {
            opportunity.setStatus("OPEN");
        } else if (!List.of("OPEN", "DRAFT").contains(opportunity.getStatus().trim().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An opportunity must be draft or open when created.");
        } else {
            opportunity.setStatus(opportunity.getStatus().trim().toUpperCase());
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
    public List<MarketplaceOpportunity> getMarketplace() {
        return opportunityRepo.findAll().stream()
                .filter(opportunity -> "OPEN".equalsIgnoreCase(opportunity.getStatus()))
                .filter(opportunity -> opportunity.getExpiresAt() == null
                        || opportunity.getExpiresAt().isAfter(java.time.LocalDateTime.now()))
                .filter(opportunity -> businessRepo.findById(opportunity.getBusiness().getId())
                        .filter(Business::isActive)
                        .filter(business -> business.getVerificationStatus() == Status.APPROVED)
                        .isPresent())
                .map(opportunity -> new MarketplaceOpportunity(
                        opportunity.getId(), opportunity.getTitle(), opportunity.getDescription(),
                        opportunity.getRequirements(), opportunity.getImageUrl(), opportunity.getTerritory() == null ? null
                                : opportunity.getTerritory().getName(),
                        opportunity.getBusiness().getId(),
                        opportunity.getBusiness().getTradeName() == null
                                ? opportunity.getBusiness().getLegalName()
                                : opportunity.getBusiness().getTradeName(),
                        opportunity.getExpiresAt()))
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
        businessAccess.requireAccess(opportunity.getBusiness().getId(), "opportunities:UPDATE");
        if (input.getBusiness() == null || input.getBusiness().getId() == null
                || !opportunity.getBusiness().getId().equals(input.getBusiness().getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "An opportunity cannot be moved to a different business.");
        }
        if ("CLOSED".equalsIgnoreCase(opportunity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A closed opportunity cannot be edited.");
        }
        businessAccess.requireAccess(input.getBusiness().getId(), "opportunities:UPDATE");
        validateTerritory(input.getTerritory(), input.getBusiness().getId());
        opportunity.setBusiness(input.getBusiness());
        opportunity.setTitle(input.getTitle());
        opportunity.setDescription(input.getDescription());
        opportunity.setImageUrl(input.getImageUrl());
        opportunity.setTerritory(input.getTerritory());
        opportunity.setRequirements(input.getRequirements());
        opportunity.setExpiresAt(input.getExpiresAt());
        return opportunityRepo.save(opportunity);
    }

    @Transactional
    public DistributionOpportiunity close(Long id) {
        DistributionOpportiunity opportunity = get(id);
        businessAccess.requireAccess(opportunity.getBusiness().getId(), "opportunities:UPDATE");
        if (!"OPEN".equalsIgnoreCase(opportunity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only an open opportunity can be closed.");
        }
        opportunity.setStatus("CLOSED");
        return opportunityRepo.save(opportunity);
    }

    private void validateTerritory(Territory territory, Long businessId) {
        if (territory == null) return;
        Territory existingTerritory = territoryRepo.findById(territory.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Territory not found."));
        if (existingTerritory.getBusiness() != null
                && !businessId.equals(existingTerritory.getBusiness().getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "The territory must belong to the opportunity's business.");
        }
    }

    public record MarketplaceOpportunity(
            Long id,
            String title,
            String description,
            String requirements,
            String imageUrl,
            String territory,
            Long businessId,
            String businessName,
            java.time.LocalDateTime expiresAt) {}
}
