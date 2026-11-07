package com.robsen.robsen.businessVerification;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.robsen.robsen.business.Business;
import com.robsen.robsen.business.BusinessRepo;
import com.robsen.robsen.businessVerificationDocument.BusinessVerificationDocument;
import com.robsen.robsen.businessVerificationDocument.BusinessVerificationDocumentRepo;
import com.robsen.robsen.common.ReviewStatus;
import com.robsen.robsen.common.Status;
import com.robsen.robsen.common.VerificationStatus;
import com.robsen.robsen.security.BusinessAccessService;

@Service
public class BusinessVerificationService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final List<String> ALLOWED_TYPES = List.of("application/pdf", "image/jpeg", "image/png");
    private final BusinessVerificationRepo verificationRepo;
    private final BusinessVerificationDocumentRepo documentRepo;
    private final BusinessRepo businessRepo;
    private final BusinessAccessService access;
    private final VerificationStorage storage;
    private final BusinessVerificationReviewRepo reviewRepo;

    public BusinessVerificationService(BusinessVerificationRepo verificationRepo,
            BusinessVerificationDocumentRepo documentRepo, BusinessRepo businessRepo,
            BusinessAccessService access, VerificationStorage storage, BusinessVerificationReviewRepo reviewRepo) {
        this.verificationRepo = verificationRepo;
        this.documentRepo = documentRepo;
        this.businessRepo = businessRepo;
        this.access = access;
        this.storage = storage;
        this.reviewRepo = reviewRepo;
    }

    @Transactional(readOnly = true)
    public VerificationView getForBusiness(Long businessId) {
        requireOwner(businessId);
        Business business = loadBusiness(businessId);
        return verificationRepo.findByBusinessId(businessId)
                .map(this::view).orElseGet(() -> new VerificationView(null, businessId,
                        business.getLegalName(), VerificationStatus.NOT_SUBMITTED, null, null, null,
                        null, null, List.of(), List.of()));
    }

    @Transactional
    public DocumentView upload(Long businessId, BusinessVerificationDocument.DocumentType type, MultipartFile file) {
        requireOwner(businessId);
        if (type == null || file == null || file.isEmpty()) throw badRequest("A document type and file are required.");
        if (file.getSize() > MAX_FILE_SIZE) throw badRequest("Documents must be 10 MB or smaller.");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT)))
            throw badRequest("Only PDF, JPEG, and PNG documents are accepted.");
        Business business = loadBusiness(businessId);
        BusinessVerification verification = verificationRepo.findByBusinessId(businessId)
                .orElseGet(() -> {
                    BusinessVerification created = new BusinessVerification();
                    created.setBusiness(business);
                    return verificationRepo.save(created);
                });
        if (verification.getStatus() == VerificationStatus.PENDING || verification.getStatus() == VerificationStatus.UNDER_REVIEW)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A submitted verification cannot be changed while under review.");
        try {
            String key = storage.store(file.getInputStream());
            BusinessVerificationDocument document = new BusinessVerificationDocument();
            document.setVerification(verification);
            document.setDocumentType(type);
            String original = Path.of(file.getOriginalFilename() == null ? "document" : file.getOriginalFilename())
                    .getFileName().toString().replaceAll("[^A-Za-z0-9._ -]", "_");
            document.setOriginalFileName(original.substring(0, Math.min(255, original.length())));
            document.setStorageKey(key);
            document.setContentType(contentType.toLowerCase(Locale.ROOT));
            document.setFileSize(file.getSize());
            document.setUploadedByUserId(access.currentUserId());
            document.setDocumentStatus(ReviewStatus.PENDING);
            verification.setUpdatedAt(LocalDateTime.now());
            return documentView(documentRepo.save(document));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The document could not be stored.");
        }
    }

    @Transactional
    public VerificationView submit(Long businessId) {
        requireOwner(businessId);
        BusinessVerification verification = verificationRepo.findByBusinessId(businessId)
                .orElseThrow(() -> badRequest("Upload the required documents before submitting."));
        List<BusinessVerificationDocument> documents = documentRepo.findByVerificationIdOrderByUploadedAtDesc(verification.getId());
        boolean resubmission = verification.getStatus() == VerificationStatus.REJECTED
                || verification.getStatus() == VerificationStatus.RESUBMISSION_REQUIRED;
        boolean license = documents.stream().anyMatch(document -> document.getDocumentType() == BusinessVerificationDocument.DocumentType.BUSINESS_LICENSE
                && (!resubmission || verification.getReviewedAt() == null || document.getUploadedAt().isAfter(verification.getReviewedAt())));
        boolean tax = documents.stream().anyMatch(document -> document.getDocumentType() == BusinessVerificationDocument.DocumentType.TAX_IDENTIFICATION
                && (!resubmission || verification.getReviewedAt() == null || document.getUploadedAt().isAfter(verification.getReviewedAt())));
        if (!license || !tax)
            throw badRequest("A business license and tax identification document are required.");
        if (verification.getStatus() == VerificationStatus.PENDING || verification.getStatus() == VerificationStatus.UNDER_REVIEW)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This verification is already being reviewed.");
        verification.setStatus(VerificationStatus.PENDING);
        verification.setSubmittedAt(LocalDateTime.now());
        verification.setReviewedAt(null);
        verification.setReviewedByUserId(null);
        verification.setRejectionReason(null);
        verification.setReviewNotes(null);
        verification.setUpdatedAt(LocalDateTime.now());
        loadBusiness(businessId).setVerificationStatus(Status.PENDING);
        return view(verificationRepo.save(verification));
    }

    @Transactional(readOnly = true)
    public Page<VerificationView> list(VerificationStatus status, String query, int page, int size) {
        requirePlatformAdmin();
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return verificationRepo.search(status, normalizedQuery,
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100))).map(this::view);
    }

    @Transactional
    public VerificationView review(Long id, ReviewDecision decision) {
        requirePlatformAdmin();
        BusinessVerification verification = verificationRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Verification request not found."));
        if (verification.getStatus() != VerificationStatus.PENDING && verification.getStatus() != VerificationStatus.UNDER_REVIEW)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only submitted verification requests can be reviewed.");
        VerificationStatus status = decision.status();
        if (status != VerificationStatus.APPROVED && status != VerificationStatus.REJECTED
                && status != VerificationStatus.RESUBMISSION_REQUIRED)
            throw badRequest("Decision must approve, reject, or request resubmission.");
        if (status == VerificationStatus.REJECTED && (decision.reason() == null || decision.reason().isBlank()))
            throw badRequest("A rejection reason is required.");
        VerificationStatus previousStatus = verification.getStatus();
        verification.setStatus(status);
        verification.setReviewedAt(LocalDateTime.now());
        verification.setReviewedByUserId(access.currentUserId());
        verification.setRejectionReason(status == VerificationStatus.REJECTED ? decision.reason().trim() : null);
        verification.setReviewNotes(decision.notes() == null ? null : decision.notes().trim());
        verification.setUpdatedAt(LocalDateTime.now());
        Business business = verification.getBusiness();
        business.setVerificationStatus(status == VerificationStatus.APPROVED ? Status.APPROVED : Status.PENDING);
        documentRepo.findByVerificationIdOrderByUploadedAtDesc(id).forEach(document -> {
            document.setDocumentStatus(status == VerificationStatus.APPROVED ? ReviewStatus.APPROVED
                    : status == VerificationStatus.REJECTED ? ReviewStatus.REJECTED : ReviewStatus.PENDING);
            document.setReviewComment(decision.notes());
        });
        BusinessVerificationReview history = new BusinessVerificationReview(); history.setVerification(verification);
        history.setPreviousStatus(previousStatus); history.setNewStatus(status); history.setReviewerUserId(access.currentUserId());
        history.setReason(verification.getRejectionReason()); history.setNotes(verification.getReviewNotes()); reviewRepo.save(history);
        return view(verificationRepo.save(verification));
    }

    @Transactional(readOnly = true)
    public StoredDocument getDocument(Long documentId) {
        BusinessVerificationDocument document = documentRepo.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found."));
        if (!access.isSuperAdmin()) requireOwner(document.getVerification().getBusiness().getId());
        try { return new StoredDocument(storage.resolve(document.getStorageKey()), document.getContentType(), document.getOriginalFileName()); }
        catch (IOException exception) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found."); }
    }

    @Transactional(readOnly = true)
    public VerificationView getForAdmin(Long id) {
        requirePlatformAdmin();
        return verificationRepo.findById(id).map(this::view)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Verification request not found."));
    }

    private VerificationView view(BusinessVerification verification) {
        return new VerificationView(verification.getId(), verification.getBusiness().getId(),
                verification.getBusiness().getLegalName(), verification.getStatus(), verification.getSubmittedAt(),
                verification.getReviewedAt(), verification.getReviewedByUserId(), verification.getRejectionReason(),
                verification.getReviewNotes(), documentRepo.findByVerificationIdOrderByUploadedAtDesc(verification.getId())
                        .stream().map(this::documentView).toList(), reviewRepo.findByVerificationIdOrderByCreatedAtAsc(verification.getId())
                        .stream().map(review -> new ReviewView(review.getPreviousStatus(), review.getNewStatus(), review.getReviewerUserId(), review.getReason(), review.getNotes(), review.getCreatedAt())).toList());
    }
    private DocumentView documentView(BusinessVerificationDocument document) {
        return new DocumentView(document.getId(), document.getDocumentType(), document.getOriginalFileName(),
                document.getContentType(), document.getFileSize(), document.getUploadedAt(), document.getDocumentStatus(), document.getReviewComment());
    }
    private Business loadBusiness(Long id) { return businessRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found.")); }
    private void requireOwner(Long id) {
        if (!access.isBusinessOwner(id)) throw new AccessDeniedException("Only the business owner can submit verification.");
    }
    private void requirePlatformAdmin() {
        if (!access.isSuperAdmin()) throw new AccessDeniedException("Platform administrator access is required.");
    }
    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }

    public record DocumentView(Long id, BusinessVerificationDocument.DocumentType documentType, String originalFileName,
            String contentType, long fileSize, LocalDateTime uploadedAt, ReviewStatus status, String reviewComment) {}
    public record VerificationView(Long id, Long businessId, String businessName, VerificationStatus status,
            LocalDateTime submittedAt, LocalDateTime reviewedAt, Long reviewedByUserId, String rejectionReason,
            String reviewNotes, List<DocumentView> documents, List<ReviewView> history) {}
    public record ReviewView(VerificationStatus previousStatus, VerificationStatus newStatus, Long reviewerUserId,
            String reason, String notes, LocalDateTime reviewedAt) {}
    public record ReviewDecision(VerificationStatus status, String reason, String notes) {}
    public record StoredDocument(Path path, String contentType, String fileName) {}
}
