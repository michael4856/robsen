package com.robsen.robsen.partnerApplication;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.robsen.robsen.common.ApplicationStatus;
import com.robsen.robsen.security.BusinessAccessService;
import com.robsen.robsen.distributionOpportunity.DistributionOpportiunity;
import com.robsen.robsen.distributionOpportunity.DistributionOpportunityRepo;
import com.robsen.robsen.business.BusinessRepo;

@Service
public class PartnerApplicationService {
    private final PartnerApplicationRepo applicationRepo;
    private final BusinessAccessService businessAccess;
    private final DistributionOpportunityRepo opportunityRepo;
    private final BusinessRepo businessRepo;

    public PartnerApplicationService(
            PartnerApplicationRepo applicationRepo,
            BusinessAccessService businessAccess,
            DistributionOpportunityRepo opportunityRepo,
            BusinessRepo businessRepo) {
        this.applicationRepo = applicationRepo;
        this.businessAccess = businessAccess;
        this.opportunityRepo = opportunityRepo;
        this.businessRepo = businessRepo;
    }

    @Transactional
    public PartnerApplication create(PartnerApplication application) {
        if (application.getApplicantBusiness() == null || application.getApplicantBusiness().getId() == null
                || application.getOpportunity() == null || application.getOpportunity().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An opportunity and applicant business are required.");
        }
        application.setId(null);
        Long applicantBusinessId = application.getApplicantBusiness().getId();
        businessAccess.requireAccess(applicantBusinessId, "partner-applications:CREATE");
        DistributionOpportiunity opportunity = opportunityRepo.findById(application.getOpportunity().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opportunity not found."));
        if (!"OPEN".equalsIgnoreCase(opportunity.getStatus())
                || (opportunity.getExpiresAt() != null && !opportunity.getExpiresAt().isAfter(LocalDateTime.now()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This opportunity is not accepting applications.");
        }
        if (opportunity.getBusiness().getId().equals(applicantBusinessId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A business cannot apply to its own opportunity.");
        }
        if (applicationRepo.existsByOpportunityIdAndApplicantBusinessId(opportunity.getId(), applicantBusinessId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This business has already applied to the opportunity.");
        }
        application.setApplicantBusiness(businessRepo.findById(applicantBusinessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Applicant business not found.")));
        application.setOpportunity(opportunity);
        application.setStatus(ApplicationStatus.PENDING);
        application.setReviewedAt(null);
        return applicationRepo.save(application);
    }

    @Transactional(readOnly = true)
    public PartnerApplication get(Long id) {
        PartnerApplication application = applicationRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found: " + id));
        requireVisible(application);
        return application;
    }

    @Transactional(readOnly = true)
    public List<PartnerApplication> getAll() {
        if (businessAccess.isSuperAdmin()) return applicationRepo.findAll();
        List<Long> businessIds = businessAccess.accessibleBusinessIds();
        if (businessIds.isEmpty()) return List.of();
        return applicationRepo.findByApplicantBusinessIdInOrOpportunityBusinessIdIn(businessIds, businessIds);
    }

    @Transactional(readOnly = true)
    public List<PartnerApplication> getByOpportunity(Long opportunityId) {
        return applicationRepo.findByOpportunityId(opportunityId).stream()
                .filter(this::isVisible)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PartnerApplication> getByBusiness(Long businessId) {
        businessAccess.requireAccess(businessId, "partner-applications:READ");
        return applicationRepo.findByApplicantBusinessId(businessId);
    }

    @Transactional(readOnly = true)
    public List<PartnerApplication> getByStatus(ApplicationStatus status) {
        return applicationRepo.findByStatus(status).stream()
                .filter(this::isVisible)
                .toList();
    }

    @Transactional
    public PartnerApplication update(Long id, PartnerApplication input) {
        PartnerApplication application = get(id);
        businessAccess.requireAccess(application.getApplicantBusiness().getId(), "partner-applications:UPDATE");
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending applications can be changed.");
        }
        application.setMessage(input.getMessage());
        if (input.getStatus() == ApplicationStatus.WITHDRAWN) {
            application.setStatus(ApplicationStatus.WITHDRAWN);
        }
        return applicationRepo.save(application);
    }

    @Transactional
    public PartnerApplication review(Long id, ApplicationStatus status) {
        if (status == null || status == ApplicationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A review status must be selected");
        }
        PartnerApplication application = get(id);
        businessAccess.requireAccess(application.getOpportunity().getBusiness().getId(), "partner-applications:UPDATE");
        if (application.getStatus() == ApplicationStatus.WITHDRAWN
                || application.getStatus() == ApplicationStatus.APPROVED
                || application.getStatus() == ApplicationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This application has already been decided or withdrawn.");
        }
        if (status == ApplicationStatus.WITHDRAWN || status == ApplicationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an application review decision.");
        }
        if (!List.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED, ApplicationStatus.REJECTED)
                .contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported application review decision.");
        }
        application.setStatus(status);
        application.setReviewedAt(LocalDateTime.now());
        return applicationRepo.save(application);
    }

    private void requireVisible(PartnerApplication application) {
        if (!isVisible(application)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have access to this partner application.");
        }
    }

    private boolean isVisible(PartnerApplication application) {
        return businessAccess.isSuperAdmin()
                || businessAccess.canAccess(
                        application.getApplicantBusiness().getId(), "partner-applications:READ")
                || businessAccess.canAccess(
                        application.getOpportunity().getBusiness().getId(), "partner-applications:READ");
    }

}
