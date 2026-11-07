package com.robsen.robsen.businessVerification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BusinessVerificationReviewRepo extends JpaRepository<BusinessVerificationReview,Long> {
 List<BusinessVerificationReview> findByVerificationIdOrderByCreatedAtAsc(Long verificationId);
}
