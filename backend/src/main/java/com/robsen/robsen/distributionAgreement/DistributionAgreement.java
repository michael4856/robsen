package com.robsen.robsen.distributionAgreement;

import java.time.LocalDate;

import com.robsen.robsen.business.Business;
import com.robsen.robsen.common.AgreementStatus;
import com.robsen.robsen.territory.Territory;

import jakarta.persistence.*;
import lombok.Data;

@Entity 
@Data 
public class DistributionAgreement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_business_id", nullable = false)
    private Business partnerBusiness;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "territory_id")
    private Territory territory;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgreementStatus status = AgreementStatus.DRAFT;

    @Column(length = 5000)
    private String terms;
}
