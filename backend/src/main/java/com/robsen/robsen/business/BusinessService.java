package com.robsen.robsen.business;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.robsen.robsen.common.BusinessNotFoundException;
import com.robsen.robsen.security.BusinessAccessService;

@Service 
public class BusinessService {
    private final BusinessRepo businessRepo;
    private final BusinessAccessService businessAccess;

    public BusinessService(BusinessRepo businessRepo, BusinessAccessService businessAccess){
        this.businessRepo = businessRepo;
        this.businessAccess = businessAccess;
    }

    @Transactional
    public Business createBusiness(Business business) {
        if (!businessAccess.isSuperAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only a platform administrator can create a business.");
        }
        business.setId(null);
        return businessRepo.save(business);
    }

    @Transactional(readOnly = true)
    public Business getBusinessById(Long id) {
        businessAccess.requireAccess(id, "businesses:READ");
        return businessRepo.findById(id)
                .orElseThrow(() -> new BusinessNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Business> getAllBusinesses(){
        return visibleBusinesses();
    }

    @Transactional(readOnly = true)
    public List<Business> getAllActiveBusinesses(){
        if (businessAccess.isSuperAdmin()) return businessRepo.findByActiveTrue();
        List<Long> businessIds = businessAccess.accessibleBusinessIds();
        if (businessIds.isEmpty()) return List.of();
        return businessRepo.findByActiveTrue().stream()
                .filter(business -> businessIds.contains(business.getId())
                        && businessAccess.canAccess(business.getId(), "businesses:READ"))
                .toList();
    }

    @Transactional
    public ResponseEntity<Business> updateBusiness(Long id, Business updatedBusiness){

        Business oldBusiness = businessRepo.findById(id)
            .orElseThrow(() -> new BusinessNotFoundException(id));
        businessAccess.requireAccess(id, "businesses:UPDATE");

        oldBusiness.setTinNumber(updatedBusiness.getTinNumber());
        oldBusiness.setLegalName(updatedBusiness.getLegalName());
        oldBusiness.setTradeName(updatedBusiness.getTradeName());
        oldBusiness.setBusinessType(updatedBusiness.getBusinessType());
        oldBusiness.setAddress(updatedBusiness.getAddress());
        oldBusiness.setPhoneNumber(updatedBusiness.getPhoneNumber());
        oldBusiness.setEmail(updatedBusiness.getEmail());
        oldBusiness.setVerificationStatus(updatedBusiness.getVerificationStatus());
        oldBusiness.setActive(updatedBusiness.isActive());

        return ResponseEntity.ok(businessRepo.save(oldBusiness));
    }

    @Transactional
    public ResponseEntity<Void> deleteBusiness(Long id){
        Business business = businessRepo.findById(id)
            .orElseThrow(() -> new BusinessNotFoundException(id));
        if (!businessAccess.isSuperAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only a platform administrator can deactivate a business.");
        }

        business.setActive(false);
        businessRepo.save(business);
        return ResponseEntity.noContent().build();
    }

    private List<Business> visibleBusinesses() {
        if (businessAccess.isSuperAdmin()) return businessRepo.findAll();
        List<Long> businessIds = businessAccess.accessibleBusinessIds();
        if (businessIds.isEmpty()) return List.of();
        return businessRepo.findAll().stream()
                .filter(business -> businessIds.contains(business.getId())
                        && businessAccess.canAccess(business.getId(), "businesses:READ"))
                .toList();
    }

}
