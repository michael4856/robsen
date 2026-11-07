package com.robsen.robsen.businessVerification;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.robsen.robsen.common.VerificationStatus;

public interface BusinessVerificationRepo extends JpaRepository<BusinessVerification, Long> {
    Optional<BusinessVerification> findByBusinessId(Long businessId);
    Page<BusinessVerification> findByStatus(VerificationStatus status, Pageable pageable);
    @Query("select verification from BusinessVerification verification where (:status is null or verification.status = :status) and (:query is null or lower(verification.business.legalName) like lower(concat('%', :query, '%')))")
    Page<BusinessVerification> search(@Param("status") VerificationStatus status, @Param("query") String query, Pageable pageable);
}
