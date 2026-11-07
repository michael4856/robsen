package com.robsen.robsen.stockMovement;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepo extends JpaRepository<StockMovement, Long> {
    Page<StockMovement> findByProductListingBusinessId(Long businessId, Pageable pageable);
    Page<StockMovement> findByProductListingId(Long listingId, Pageable pageable);
}
