package com.robsen.robsen.quotation;

import java.time.LocalDateTime;
import com.robsen.robsen.business.Business;
import jakarta.persistence.*;
import lombok.Data;

@Data @Entity @Table(name = "quotation_requests", uniqueConstraints = @UniqueConstraint(name="uk_rfq_number", columnNames="rfq_number"))
public class QuotationRequest {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="rfq_number", nullable=false, unique=true, length=40) private String rfqNumber;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="buyer_business_id", nullable=false) private Business buyerBusiness;
    @Column(nullable=false, length=255) private String title;
    @Column(length=4000) private String description;
    @Column(nullable=false, length=24) private String status="DRAFT";
    private LocalDateTime responseDeadline;
    @Column(length=1000) private String deliveryLocation;
    private Long acceptedQuotationId;
    @Column(nullable=false) private Long createdByUserId;
    @Column(nullable=false, updatable=false) private LocalDateTime createdAt=LocalDateTime.now();
}
