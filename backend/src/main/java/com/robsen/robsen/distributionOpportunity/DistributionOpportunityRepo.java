package com.robsen.robsen.distributionOpportunity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DistributionOpportunityRepo extends JpaRepository<DistributionOpportiunity, Long> {
    List<DistributionOpportiunity> findByBusinessId(Long businessId);
    List<DistributionOpportiunity> findByBusinessIdIn(java.util.Collection<Long> businessIds);
    List<DistributionOpportiunity> findByTerritoryId(Long territoryId);
}
