package com.robsen.robsen.territory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TerritoryRepo extends JpaRepository<Territory, Long> {
    java.util.List<Territory> findByBusinessIdIn(java.util.Collection<Long> businessIds);
    java.util.List<Territory> findByBusinessId(Long businessId);
}
