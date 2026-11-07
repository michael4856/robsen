package com.robsen.robsen.businessVerification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.mock.web.MockMultipartFile;

import com.robsen.robsen.business.Business;
import com.robsen.robsen.business.BusinessRepo;
import com.robsen.robsen.businessVerificationDocument.BusinessVerificationDocument;
import com.robsen.robsen.businessVerificationDocument.BusinessVerificationDocumentRepo;
import com.robsen.robsen.common.ReviewStatus;
import com.robsen.robsen.common.Status;
import com.robsen.robsen.common.VerificationStatus;
import com.robsen.robsen.security.BusinessAccessService;

class BusinessVerificationServiceTest {
    private final BusinessVerificationRepo verificationRepo = mock(BusinessVerificationRepo.class);
    private final BusinessVerificationDocumentRepo documentRepo = mock(BusinessVerificationDocumentRepo.class);
    private final BusinessRepo businessRepo = mock(BusinessRepo.class);
    private final BusinessAccessService access = mock(BusinessAccessService.class);
    private final VerificationStorage storage = mock(VerificationStorage.class);
    private final BusinessVerificationReviewRepo reviewRepo = mock(BusinessVerificationReviewRepo.class);
    private final BusinessVerificationService service = new BusinessVerificationService(
            verificationRepo, documentRepo, businessRepo, access, storage, reviewRepo);
    private final Business business = new Business();
    private final BusinessVerification verification = new BusinessVerification();
    private final List<BusinessVerificationDocument> documents = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        business.setId(11L);
        business.setLegalName("Northwind Supply");
        business.setVerificationStatus(Status.PENDING);
        verification.setId(21L);
        verification.setBusiness(business);
        verification.setStatus(VerificationStatus.NOT_SUBMITTED);
        when(access.isBusinessOwner(11L)).thenReturn(true);
        when(access.currentUserId()).thenReturn(7L);
        when(businessRepo.findById(11L)).thenReturn(Optional.of(business));
        when(verificationRepo.findByBusinessId(11L)).thenReturn(Optional.of(verification));
        when(verificationRepo.findById(21L)).thenReturn(Optional.of(verification));
        when(verificationRepo.save(any(BusinessVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(documentRepo.findByVerificationIdOrderByUploadedAtDesc(21L)).thenReturn(documents);
        when(documentRepo.save(any(BusinessVerificationDocument.class)))
                .thenAnswer(invocation -> {
                    BusinessVerificationDocument document = invocation.getArgument(0);
                    document.setId((long) documents.size() + 1);
                    documents.add(document);
                    return document;
                });
        when(storage.store(any(InputStream.class))).thenAnswer(invocation -> "verification-document-" + documents.size());
        when(reviewRepo.findByVerificationIdOrderByCreatedAtAsc(21L)).thenReturn(List.of());
    }

    @Test
    void ownerSubmitsRequiredDocumentsAndAdminApprovalUpdatesBusinessAndReviewHistory() {
        service.upload(11L, BusinessVerificationDocument.DocumentType.BUSINESS_LICENSE,
                file("license.pdf", "application/pdf"));
        service.upload(11L, BusinessVerificationDocument.DocumentType.TAX_IDENTIFICATION,
                file("tax-id.png", "image/png"));
        assertEquals(2, documents.size());

        BusinessVerificationService.VerificationView submitted = service.submit(11L);

        assertEquals(VerificationStatus.PENDING, submitted.status());
        assertNotNull(submitted.submittedAt());
        assertEquals(Status.PENDING, business.getVerificationStatus());

        when(access.isSuperAdmin()).thenReturn(true);
        BusinessVerificationService.VerificationView reviewed = service.review(21L,
                new BusinessVerificationService.ReviewDecision(VerificationStatus.APPROVED, null, "Documents verified"));

        assertEquals(VerificationStatus.APPROVED, reviewed.status());
        assertEquals(Status.APPROVED, business.getVerificationStatus());
        assertEquals(ReviewStatus.APPROVED, documents.getFirst().getDocumentStatus());
        verify(reviewRepo).save(any(BusinessVerificationReview.class));
    }

    @Test
    void submissionRequiresBothCurrentDocumentTypes() {
        service.upload(11L, BusinessVerificationDocument.DocumentType.BUSINESS_LICENSE,
                file("license.pdf", "application/pdf"));

        var exception = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.submit(11L));

        assertEquals(400, exception.getStatusCode().value());
        assertEquals(VerificationStatus.NOT_SUBMITTED, verification.getStatus());
    }

    @Test
    void rejectionRequiresAReason() {
        when(access.isSuperAdmin()).thenReturn(true);
        verification.setStatus(VerificationStatus.PENDING);

        var exception = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.review(21L,
                        new BusinessVerificationService.ReviewDecision(VerificationStatus.REJECTED, "  ", null)));

        assertEquals(400, exception.getStatusCode().value());
        assertEquals(VerificationStatus.PENDING, verification.getStatus());
    }

    @Test
    void nonOwnerCannotReadBusinessVerification() {
        when(access.isBusinessOwner(11L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.getForBusiness(11L));
    }

    private static MockMultipartFile file(String name, String contentType) {
        return new MockMultipartFile("file", name, contentType, new byte[] {1, 2, 3});
    }
}
