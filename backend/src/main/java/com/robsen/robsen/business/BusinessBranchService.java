package com.robsen.robsen.business;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.robsen.robsen.common.BusinessNotFoundException;
import com.robsen.robsen.security.BusinessAccessService;

@Service
public class BusinessBranchService {
    private final BusinessRepo businessRepo;
    private final BusinessBranchRepo branchRepo;
    private final BusinessAccessService businessAccess;

    public BusinessBranchService(
            BusinessRepo businessRepo,
            BusinessBranchRepo branchRepo,
            BusinessAccessService businessAccess) {
        this.businessRepo = businessRepo;
        this.branchRepo = branchRepo;
        this.businessAccess = businessAccess;
    }

    @Transactional(readOnly = true)
    public List<BranchView> list(Long businessId) {
        businessAccess.requireAccess(businessId, "businesses:READ");
        return branchRepo.findByBusinessIdAndActiveTrueOrderByNameAsc(businessId).stream()
                .map(BusinessBranchService::toView)
                .toList();
    }

    @Transactional
    public BranchView create(Long businessId, BusinessBranchController.BranchRequest request) {
        businessAccess.requireAccess(businessId, "businesses:UPDATE");
        Business business = businessRepo.findById(businessId)
                .orElseThrow(() -> new BusinessNotFoundException(businessId));
        BusinessBranch branch = new BusinessBranch();
        branch.setBusiness(business);
        apply(branch, request);
        return toView(branchRepo.save(branch));
    }

    @Transactional
    public BranchView update(
            Long businessId,
            Long branchId,
            BusinessBranchController.BranchRequest request) {
        businessAccess.requireAccess(businessId, "businesses:UPDATE");
        BusinessBranch branch = findBranch(businessId, branchId);
        apply(branch, request);
        return toView(branchRepo.save(branch));
    }

    @Transactional
    public void deactivate(Long businessId, Long branchId) {
        businessAccess.requireAccess(businessId, "businesses:UPDATE");
        BusinessBranch branch = findBranch(businessId, branchId);
        branch.setActive(false);
        branchRepo.save(branch);
    }

    private BusinessBranch findBranch(Long businessId, Long branchId) {
        return branchRepo.findByIdAndBusinessId(branchId, businessId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Branch not found for this business."));
    }

    private static void apply(BusinessBranch branch, BusinessBranchController.BranchRequest request) {
        branch.setName(request.name().trim());
        branch.setAddress(request.address().trim());
        branch.setCity(trimToNull(request.city()));
        branch.setRegion(trimToNull(request.region()));
        branch.setPhoneNumber(trimToNull(request.phoneNumber()));
        branch.setEmail(trimToNull(request.email()));
    }

    private static BranchView toView(BusinessBranch branch) {
        return new BranchView(
                branch.getId(),
                branch.getBusiness().getId(),
                branch.getName(),
                branch.getAddress(),
                branch.getCity(),
                branch.getRegion(),
                branch.getPhoneNumber(),
                branch.getEmail());
    }

    public record BranchView(
            Long id,
            Long businessId,
            String name,
            String address,
            String city,
            String region,
            String phoneNumber,
            String email) {}

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
