package com.robsen.robsen.business;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.robsen.robsen.common.Status;

@Repository 
public interface BusinessRepo extends JpaRepository<Business, Long>{
    java.util.List<Business> findByActiveTrue();
    java.util.List<Business> findByActiveTrueAndVerificationStatus(Status verificationStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select business from Business business where business.id = :id")
    java.util.Optional<Business> findByIdForUpdate(@Param("id") Long id);
}
