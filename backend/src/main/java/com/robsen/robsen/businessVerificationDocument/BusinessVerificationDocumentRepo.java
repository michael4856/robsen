package com.robsen.robsen.businessVerificationDocument;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessVerificationDocumentRepo extends JpaRepository<BusinessVerificationDocument, Long> {
    List<BusinessVerificationDocument> findByVerificationIdOrderByUploadedAtDesc(Long verificationId);
    boolean existsByVerificationIdAndDocumentType(Long verificationId, BusinessVerificationDocument.DocumentType type);
}
