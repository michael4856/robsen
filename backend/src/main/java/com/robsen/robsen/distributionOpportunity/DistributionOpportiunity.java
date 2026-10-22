package com.robsen.robsen.distributionOpportunity;

import java.time.LocalDateTime;

import com.robsen.robsen.business.Business;
import com.robsen.robsen.territory.Territory;

import jakarta.persistence.*;
import lombok.Data;

@Entity 
@Data 
public class DistributionOpportiunity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "business_id")
    private Business business;
    private String title;
    private String description;
    @ManyToOne
    @JoinColumn(name = "territory_id")
    private Territory territory;
    private String requirements;
    private String status;
    private LocalDateTime expiresAt;
}
