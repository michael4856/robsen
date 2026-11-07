package com.robsen.robsen.quotation;

import com.robsen.robsen.product.Product;
import jakarta.persistence.*;
import lombok.Data;

@Data @Entity @Table(name="quotation_request_items", indexes=@Index(name="idx_rfq_items_request", columnList="request_id"))
public class QuotationRequestItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="request_id", nullable=false) private QuotationRequest request;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="product_id", nullable=false) private Product product;
    @Column(nullable=false) private int quantity;
    @Column(length=2000) private String specifications;
}
