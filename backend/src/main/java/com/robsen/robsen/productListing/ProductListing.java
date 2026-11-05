package com.robsen.robsen.productListing;

import java.math.BigDecimal;

import com.robsen.robsen.business.Business;
import com.robsen.robsen.product.Product;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity 
@Data 
public class ProductListing {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    private int availableQuantity;
    private int minimumOrderQuantity;
    private BigDecimal wholesalePrice;
    private String description;
    private boolean active;
}
