package com.robsen.robsen.partnerApplication;

import java.time.LocalDateTime;

import com.robsen.robsen.business.Business;
import com.robsen.robsen.common.ApplicationStatus;
import com.robsen.robsen.distributionOpportunity.DistributionOpportiunity;

import jakarta.persistence.*;
import jakarta.persistence.Id;
import lombok.Data;

@Entity 
@Data 
public class PartnerApplication {
    //id, opportunityId, applicantBusinessId, message, status, reviewedAt
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne 
    @JoinColumn (name = "distribution_opportunity_id")
    private DistributionOpportiunity opportunity;

    @ManyToOne 
    @JoinColumn 
    private Business applicantBusiness;
    private String message;
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;
    private LocalDateTime reviewedAt;

}
