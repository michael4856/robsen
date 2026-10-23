package com.robsen.robsen.distributionAgreement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.robsen.robsen.common.AgreementStatus;

public interface DistributionAgreementRepo extends JpaRepository<DistributionAgreement, Long> {
    List<DistributionAgreement> findByPartnerBusinessId(Long businessId);
    List<DistributionAgreement> findByPartnerBusinessIdIn(java.util.Collection<Long> businessIds);
    List<DistributionAgreement> findByStatus(AgreementStatus status);
}
