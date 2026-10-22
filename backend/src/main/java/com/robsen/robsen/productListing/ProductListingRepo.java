package com.robsen.robsen.productListing;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductListingRepo extends JpaRepository<ProductListing, Long> {
    List<ProductListing> findByActiveTrue();
    List<ProductListing> findByBusinessId(Long businessId);
    List<ProductListing> findByProductId(Long productId);
}
