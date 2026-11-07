package com.robsen.robsen.quotation;

import java.time.LocalDateTime;
import com.robsen.robsen.business.Business;
import jakarta.persistence.*;
import lombok.Data;

@Data @Entity @Table(name="supplier_quotations", uniqueConstraints=@UniqueConstraint(name="uk_quotation_number", columnNames="quotation_number"), indexes=@Index(name="idx_quotations_request_status", columnList="request_id, status"))
public class SupplierQuotation {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="quotation_number", nullable=false, unique=true, length=40) private String quotationNumber;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="request_id", nullable=false) private QuotationRequest request;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="supplier_business_id", nullable=false) private Business supplierBusiness;
    @Column(nullable=false, length=24) private String status="DRAFT";
    private LocalDateTime validUntil;
    private int deliveryLeadDays;
    @Column(length=1000) private String paymentTerms;
    @Column(length=1000) private String deliveryTerms;
    @Column(length=4000) private String remarks;
    private Long previousVersionId;
    @Column(nullable=false) private Long createdByUserId;
    @Column(nullable=false, updatable=false) private LocalDateTime createdAt=LocalDateTime.now();
    private LocalDateTime submittedAt;
}
