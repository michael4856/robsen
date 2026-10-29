package com.robsen.robsen.business;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.robsen.robsen.common.BusinessNotFoundException;
import com.robsen.robsen.businessMembership.BusinessMembership;
import com.robsen.robsen.businessMembership.BusinessMembershipRepo;
import com.robsen.robsen.common.MembershipStatus;
import com.robsen.robsen.role.Role;
import com.robsen.robsen.role.RoleRepo;
import com.robsen.robsen.security.BusinessAccessService;
import com.robsen.robsen.role.BusinessRolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;

@Service 
public class BusinessService {
    private final BusinessRepo businessRepo;
    private final BusinessAccessService businessAccess;
    private final BusinessMembershipRepo membershipRepo;
    private final RoleRepo roleRepo;
    private final BusinessDashboardService dashboardService;
    private final BusinessRolePermissionService businessRolePermissionService;

    public BusinessService(
            BusinessRepo businessRepo,
            BusinessAccessService businessAccess,
            BusinessMembershipRepo membershipRepo,
            RoleRepo roleRepo,
            BusinessDashboardService dashboardService) {
        this(businessRepo, businessAccess, membershipRepo, roleRepo, dashboardService, null);
    }

    @Autowired
    public BusinessService(
            BusinessRepo businessRepo,
            BusinessAccessService businessAccess,
            BusinessMembershipRepo membershipRepo,
            RoleRepo roleRepo,
            BusinessDashboardService dashboardService,
            BusinessRolePermissionService businessRolePermissionService) {
        this.businessRepo = businessRepo;
        this.businessAccess = businessAccess;
        this.membershipRepo = membershipRepo;
        this.roleRepo = roleRepo;
        this.dashboardService = dashboardService;
        this.businessRolePermissionService = businessRolePermissionService;
    }

    @Transactional(readOnly = true)
    public BusinessDashboardService.DashboardStats getDashboard(Long businessId) {
        businessAccess.requireAccess(businessId);
        return dashboardService.get(businessId, permission -> businessAccess.canAccess(businessId, permission));
    }

    @Transactional(readOnly = true)
    public List<MyBusiness> getMyBusinesses() {
        return membershipRepo.findByUserIdAndStatus(
                        businessAccess.currentUserId(), MembershipStatus.ACTIVE)
                .stream()
                .filter(membership -> membership.getBusiness() != null
                        && membership.getBusiness().isActive())
                .map(membership -> new MyBusiness(
                        membership.getBusiness().getId(),
                        membership.getBusiness().getLegalName(),
                        membership.getBusiness().getTradeName(),
                        membership.getBusiness().getBusinessType(),
                        membership.getBusiness().getVerificationStatus(),
                        membership.getId(),
                        membership.getRole() == null ? null : membership.getRole().getName(),
                        membership.getStatus()))
                .toList();
    }

    @Transactional
    public Business createBusiness(Business business) {
        if (business.getLegalName() == null || business.getLegalName().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Business legal name is required.");
        }
        business.setId(null);
        business.setLegalName(business.getLegalName().trim());
        business.setVerificationStatus(com.robsen.robsen.common.Status.PENDING);
        business.setActive(true);
        Business saved = businessRepo.save(business);

        Role ownerRole = roleRepo.findByNameIgnoreCaseAndActiveTrue("OWNER")
                .orElseThrow(() -> new IllegalStateException("The OWNER business role is not configured."));
        BusinessMembership ownerMembership = new BusinessMembership();
        ownerMembership.setUser(businessAccess.currentUserEntity());
        ownerMembership.setBusiness(saved);
        ownerMembership.setRole(ownerRole);
        ownerMembership.setStatus(MembershipStatus.ACTIVE);
        membershipRepo.save(ownerMembership);
        if (businessRolePermissionService != null) {
            businessRolePermissionService.initializeBusiness(saved.getId());
        }
        return saved;
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
        oldBusiness.setDescription(updatedBusiness.getDescription());
        oldBusiness.setBusinessType(updatedBusiness.getBusinessType());
        oldBusiness.setAddress(updatedBusiness.getAddress());
        oldBusiness.setPhoneNumber(updatedBusiness.getPhoneNumber());
        oldBusiness.setEmail(updatedBusiness.getEmail());
        if (businessAccess.isSuperAdmin()) {
            oldBusiness.setVerificationStatus(updatedBusiness.getVerificationStatus());
            oldBusiness.setActive(updatedBusiness.isActive());
        }
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

    public record MyBusiness(
            Long id,
            String legalName,
            String tradeName,
            String businessType,
            com.robsen.robsen.common.Status verificationStatus,
            Long membershipId,
            String membershipRole,
            MembershipStatus membershipStatus) {}

}
