package com.robsen.robsen.business;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.robsen.robsen.common.BusinessNotFoundException;

@Service 
public class BusinessService {
    private final BusinessRepo businessRepo;

    public BusinessService(BusinessRepo businessRepo){
        this.businessRepo = businessRepo;
    }

    @Transactional
    public Business createBusiness(Business business) {
        business.setId(null);
        return businessRepo.save(business);
    }

    @Transactional(readOnly = true)
    public Business getBusinessById(Long id) {
        return businessRepo.findById(id)
                .orElseThrow(() -> new BusinessNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Business> getAllBusinesses(){
        return businessRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<Business> getAllActiveBusinesses(){
        return businessRepo.findByActiveTrue();
    }

    @Transactional
    public ResponseEntity<Business> updateBusiness(Long id, Business updatedBusiness){

        Business oldBusiness = businessRepo.findById(id)
            .orElseThrow(() -> new BusinessNotFoundException(id));

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

        business.setActive(false);
        businessRepo.save(business);
        return ResponseEntity.noContent().build();
    }

}
