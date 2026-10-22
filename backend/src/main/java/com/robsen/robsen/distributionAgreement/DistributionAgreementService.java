package com.robsen.robsen.distributionAgreement;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.robsen.robsen.common.AgreementStatus;

@Service
public class DistributionAgreementService {
    private final DistributionAgreementRepo agreementRepo;

    public DistributionAgreementService(DistributionAgreementRepo agreementRepo) {
        this.agreementRepo = agreementRepo;
    }

    @Transactional
    public DistributionAgreement create(DistributionAgreement agreement) {
        agreement.setId(null);
        return agreementRepo.save(agreement);
    }

    @Transactional(readOnly = true)
    public DistributionAgreement get(Long id) {
        return agreementRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agreement not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<DistributionAgreement> getAll() {
        return agreementRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<DistributionAgreement> getByBusiness(Long businessId) {
        return agreementRepo.findByPartnerBusinessId(businessId);
    }

    @Transactional(readOnly = true)
    public List<DistributionAgreement> getByStatus(AgreementStatus status) {
        return agreementRepo.findByStatus(status);
    }

    @Transactional
    public DistributionAgreement update(Long id, DistributionAgreement input) {
        DistributionAgreement agreement = get(id);
        agreement.setPartnerBusiness(input.getPartnerBusiness());
        agreement.setTerritory(input.getTerritory());
        agreement.setStartDate(input.getStartDate());
        agreement.setEndDate(input.getEndDate());
        agreement.setStatus(input.getStatus());
        agreement.setTerms(input.getTerms());
        return agreementRepo.save(agreement);
    }

    @Transactional
    public DistributionAgreement terminate(Long id) {
        DistributionAgreement agreement = get(id);
        agreement.setStatus(AgreementStatus.TERMINATED);
        return agreementRepo.save(agreement);
    }
}
