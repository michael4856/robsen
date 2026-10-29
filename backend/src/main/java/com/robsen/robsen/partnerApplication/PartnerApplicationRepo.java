package com.robsen.robsen.partnerApplication;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.robsen.robsen.common.ApplicationStatus;

public interface PartnerApplicationRepo extends JpaRepository<PartnerApplication, Long> {
    List<PartnerApplication> findByOpportunityId(Long opportunityId);
    List<PartnerApplication> findByApplicantBusinessId(Long businessId);
    boolean existsByOpportunityIdAndApplicantBusinessId(Long opportunityId, Long applicantBusinessId);
    List<PartnerApplication> findByStatus(ApplicationStatus status);
    List<PartnerApplication> findByApplicantBusinessIdInOrOpportunityBusinessIdIn(
            java.util.Collection<Long> applicantBusinessIds, java.util.Collection<Long> opportunityBusinessIds);
}
