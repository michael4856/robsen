package com.robsen.robsen.business;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessBranchRepo extends JpaRepository<BusinessBranch, Long> {
    List<BusinessBranch> findByBusinessIdAndActiveTrueOrderByNameAsc(Long businessId);
    Optional<BusinessBranch> findByIdAndBusinessId(Long id, Long businessId);
}
