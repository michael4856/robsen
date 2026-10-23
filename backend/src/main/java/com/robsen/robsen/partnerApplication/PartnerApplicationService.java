package com.robsen.robsen.partnerApplication;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.robsen.robsen.common.ApplicationStatus;
import com.robsen.robsen.security.BusinessAccessService;

@Service
public class PartnerApplicationService {
    private final PartnerApplicationRepo applicationRepo;
    private final BusinessAccessService businessAccess;

    public PartnerApplicationService(PartnerApplicationRepo applicationRepo, BusinessAccessService businessAccess) {
        this.applicationRepo = applicationRepo;
        this.businessAccess = businessAccess;
    }

    @Transactional
    public PartnerApplication create(PartnerApplication application) {
        application.setId(null);
        businessAccess.requireAccess(application.getApplicantBusiness().getId(), "partner-applications:CREATE");
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
        if (!businessAccess.canAccess(
                        application.getApplicantBusiness().getId(), "partner-applications:UPDATE")
                && !businessAccess.canAccess(
                        application.getOpportunity().getBusiness().getId(), "partner-applications:UPDATE")) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Your business role does not grant partner application updates.");
        }
        if (!application.getApplicantBusiness().getId().equals(input.getApplicantBusiness().getId())) {
            businessAccess.requireAccess(input.getApplicantBusiness().getId(), "partner-applications:UPDATE");
        }
        if (!application.getOpportunity().getBusiness().getId()
                .equals(input.getOpportunity().getBusiness().getId())) {
            businessAccess.requireAccess(input.getOpportunity().getBusiness().getId(), "partner-applications:UPDATE");
        }
        if (!businessAccess.canAccess(input.getApplicantBusiness().getId(), "partner-applications:UPDATE")
                && !businessAccess.canAccess(
                        input.getOpportunity().getBusiness().getId(), "partner-applications:UPDATE")) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "The application must remain associated with an accessible business.");
        }
        application.setOpportunity(input.getOpportunity());
        application.setApplicantBusiness(input.getApplicantBusiness());
        application.setMessage(input.getMessage());
        application.setStatus(input.getStatus());
        application.setReviewedAt(input.getReviewedAt());
        return applicationRepo.save(application);
    }

    @Transactional
    public PartnerApplication review(Long id, ApplicationStatus status) {
        if (status == null || status == ApplicationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A review status must be selected");
        }
        PartnerApplication application = get(id);
        if (!businessAccess.canAccess(application.getApplicantBusiness().getId(), "partner-applications:UPDATE")
                && !businessAccess.canAccess(
                        application.getOpportunity().getBusiness().getId(), "partner-applications:UPDATE")) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Your business role does not grant partner application updates.");
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
