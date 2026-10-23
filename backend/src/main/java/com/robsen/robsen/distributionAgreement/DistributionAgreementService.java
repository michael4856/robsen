package com.robsen.robsen.distributionAgreement;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.robsen.robsen.common.AgreementStatus;
import com.robsen.robsen.security.BusinessAccessService;
import com.robsen.robsen.territory.Territory;
import com.robsen.robsen.territory.TerritoryRepo;

@Service
public class DistributionAgreementService {
    private final DistributionAgreementRepo agreementRepo;
    private final BusinessAccessService businessAccess;
    private final TerritoryRepo territoryRepo;

    public DistributionAgreementService(
            DistributionAgreementRepo agreementRepo,
            BusinessAccessService businessAccess,
            TerritoryRepo territoryRepo) {
        this.agreementRepo = agreementRepo;
        this.businessAccess = businessAccess;
        this.territoryRepo = territoryRepo;
    }

    @Transactional
    public DistributionAgreement create(DistributionAgreement agreement) {
        agreement.setId(null);
        businessAccess.requireAccess(agreement.getPartnerBusiness().getId(), "agreements:CREATE");
        validateTerritory(agreement.getTerritory(), agreement.getPartnerBusiness().getId());
        return agreementRepo.save(agreement);
    }

    @Transactional(readOnly = true)
    public DistributionAgreement get(Long id) {
        DistributionAgreement agreement = agreementRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agreement not found: " + id));
        businessAccess.requireAccess(agreement.getPartnerBusiness().getId(), "agreements:READ");
        return agreement;
    }

    @Transactional(readOnly = true)
    public List<DistributionAgreement> getAll() {
        if (businessAccess.isSuperAdmin()) return agreementRepo.findAll();
        List<Long> businessIds = businessAccess.accessibleBusinessIds();
        if (businessIds.isEmpty()) return List.of();
        return agreementRepo.findByPartnerBusinessIdIn(businessIds).stream()
                .filter(agreement -> businessAccess.canAccess(
                        agreement.getPartnerBusiness().getId(), "agreements:READ"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DistributionAgreement> getByBusiness(Long businessId) {
        businessAccess.requireAccess(businessId, "agreements:READ");
        return agreementRepo.findByPartnerBusinessId(businessId);
    }

    @Transactional(readOnly = true)
    public List<DistributionAgreement> getByStatus(AgreementStatus status) {
        return agreementRepo.findByStatus(status).stream()
                .filter(agreement -> businessAccess.canAccess(
                        agreement.getPartnerBusiness().getId(), "agreements:READ"))
                .toList();
    }

    @Transactional
    public DistributionAgreement update(Long id, DistributionAgreement input) {
        DistributionAgreement agreement = get(id);
        businessAccess.requireAccess(agreement.getPartnerBusiness().getId(), "agreements:UPDATE");
        businessAccess.requireAccess(input.getPartnerBusiness().getId(), "agreements:UPDATE");
        validateTerritory(input.getTerritory(), input.getPartnerBusiness().getId());
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
        businessAccess.requireAccess(agreement.getPartnerBusiness().getId(), "agreements:DELETE");
        agreement.setStatus(AgreementStatus.TERMINATED);
        return agreementRepo.save(agreement);
    }

    private void validateTerritory(Territory territory, Long businessId) {
        if (territory == null) return;
        Territory existingTerritory = territoryRepo.findById(territory.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Territory not found."));
        if (!businessAccess.isSuperAdmin()
                && (existingTerritory.getBusiness() == null
                        || !businessId.equals(existingTerritory.getBusiness().getId()))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "The territory must belong to the agreement's business.");
        }
    }
}
