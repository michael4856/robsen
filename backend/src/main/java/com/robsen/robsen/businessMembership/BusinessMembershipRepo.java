package com.robsen.robsen.businessMembership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import com.robsen.robsen.common.MembershipStatus;

@Repository 
public interface BusinessMembershipRepo extends JpaRepository<BusinessMembership, Long>{
    List<BusinessMembership> findByUserId(Long userId);
    List<BusinessMembership> findByUserIdAndStatus(Long userId, MembershipStatus status);
    java.util.Optional<BusinessMembership> findByUserIdAndBusinessIdAndStatus(
            Long userId, Long businessId, MembershipStatus status);
    List<BusinessMembership> findByBusinessId(Long businessId);
    List<BusinessMembership> findByBusinessIdIn(java.util.Collection<Long> businessIds);
}
