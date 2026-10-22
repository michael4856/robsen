package com.robsen.robsen.orderItem;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;

import java.math.BigDecimal;

import com.robsen.robsen.customerOrder.CustomerOrder;
import com.robsen.robsen.productListing.ProductListing;

import jakarta.persistence.*;
import lombok.Data;

@Entity 
@Data 
public class OrderItem {
    //id, orderId, productListingId, quantity, unitPrice, lineTotal

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "customer_order_id")
    private CustomerOrder order;
    @ManyToOne
    @JoinColumn(name = "product_listing_id")
    private ProductListing productListing;  
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
