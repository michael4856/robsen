package com.robsen.robsen.businessMembership;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import com.robsen.robsen.business.Business;
import com.robsen.robsen.common.MembershipStatus;
import com.robsen.robsen.role.Role;
import com.robsen.robsen.user.User;

import jakarta.persistence.*;
import lombok.Data;

@Entity 
@Data 

public class BusinessMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    // Assumes Role is an @Entity in your project.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    private LocalDateTime invitedAt;

    private LocalDateTime expiresAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "business_membership_permission_grants", joinColumns = @JoinColumn(name = "membership_id"))
    @Column(name = "permission", nullable = false, length = 100)
    private Set<String> permissionGrants = new LinkedHashSet<>();

    
      
}
