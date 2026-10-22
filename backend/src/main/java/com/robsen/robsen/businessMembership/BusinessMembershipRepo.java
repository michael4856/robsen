package com.robsen.robsen.businessMembership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository 
public interface BusinessMembershipRepo extends JpaRepository<BusinessMembership, Long>{
    List<BusinessMembership> findByUserId(Long userId);
    List<BusinessMembership> findByBusinessId(Long businessId);
}
