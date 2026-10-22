package com.robsen.robsen.customerOrder;

import java.time.LocalDateTime;

import com.robsen.robsen.business.Business;

import jakarta.persistence.*;
import lombok.Data;

@Entity 
@Data 
public class CustomerOrder {
    //id, buyerBusinessId, supplierBusinessId, orderNumber, status, subtotal, createdAt
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "buyer_business_id")
    private Business buyerBusiness;
    @ManyToOne
    @JoinColumn(name = "supplier_business_id")
    private Business supplierBusiness;
    private String orderNumber;
    private String status;
    private double subtotal;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
