package com.robsen.robsen.businessMembership;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service 
public class BusinessMembershipService {
    private final BusinessMembershipRepo businessMembershipRepo;

    public BusinessMembershipService(BusinessMembershipRepo businessMembershipRepo){
        this.businessMembershipRepo = businessMembershipRepo;
    }

    @Transactional
    public BusinessMembership create(BusinessMembership membership) {
        membership.setId(null);
        if (membership.getJoinedAt() == null) {
            membership.setJoinedAt(LocalDateTime.now());
        }
        return businessMembershipRepo.save(membership);
    }

    @Transactional(readOnly = true)
    public BusinessMembership get(Long id) {
        return businessMembershipRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<BusinessMembership> getAll() {
        return businessMembershipRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<BusinessMembership> getByUser(Long userId) {
        return businessMembershipRepo.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<BusinessMembership> getByBusiness(Long businessId) {
        return businessMembershipRepo.findByBusinessId(businessId);
    }

    @Transactional
    public BusinessMembership update(Long id, BusinessMembership input) {
        BusinessMembership membership = get(id);
        membership.setUser(input.getUser());
        membership.setBusiness(input.getBusiness());
        membership.setRole(input.getRole());
        membership.setStatus(input.getStatus());
        return businessMembershipRepo.save(membership);
    }

    @Transactional
    public void delete(Long id) {
        BusinessMembership membership = get(id);
        membership.setStatus(com.robsen.robsen.common.MembershipStatus.REMOVED);
        businessMembershipRepo.save(membership);
    }
}
