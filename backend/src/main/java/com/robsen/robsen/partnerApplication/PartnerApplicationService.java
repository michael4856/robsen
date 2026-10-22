package com.robsen.robsen.partnerApplication;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.robsen.robsen.common.ApplicationStatus;

@Service
public class PartnerApplicationService {
    private final PartnerApplicationRepo applicationRepo;

    public PartnerApplicationService(PartnerApplicationRepo applicationRepo) {
        this.applicationRepo = applicationRepo;
    }

    @Transactional
    public PartnerApplication create(PartnerApplication application) {
        application.setId(null);
        application.setStatus(ApplicationStatus.PENDING);
        application.setReviewedAt(null);
        return applicationRepo.save(application);
    }

    @Transactional(readOnly = true)
    public PartnerApplication get(Long id) {
        return applicationRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<PartnerApplication> getAll() {
        return applicationRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<PartnerApplication> getByOpportunity(Long opportunityId) {
        return applicationRepo.findByOpportunityId(opportunityId);
    }

    @Transactional(readOnly = true)
    public List<PartnerApplication> getByBusiness(Long businessId) {
        return applicationRepo.findByApplicantBusinessId(businessId);
    }

    @Transactional(readOnly = true)
    public List<PartnerApplication> getByStatus(ApplicationStatus status) {
        return applicationRepo.findByStatus(status);
    }

    @Transactional
    public PartnerApplication update(Long id, PartnerApplication input) {
        PartnerApplication application = get(id);
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
        application.setStatus(status);
        application.setReviewedAt(LocalDateTime.now());
        return applicationRepo.save(application);
    }
}
