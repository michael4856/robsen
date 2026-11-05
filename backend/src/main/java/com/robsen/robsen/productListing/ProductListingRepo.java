package com.robsen.robsen.productListing;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ProductListingRepo extends JpaRepository<ProductListing, Long> {
    List<ProductListing> findByActiveTrue();
    List<ProductListing> findByBusinessId(Long businessId);
    List<ProductListing> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select listing from ProductListing listing where listing.id = :id")
    java.util.Optional<ProductListing> findByIdForUpdate(@Param("id") Long id);
}
