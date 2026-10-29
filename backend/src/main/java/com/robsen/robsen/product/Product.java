package com.robsen.robsen.product;

import com.robsen.robsen.productCategory.ProductCategory;
import com.robsen.robsen.business.Business;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity 
@Data 
public class Product {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "product_category_id")
    private ProductCategory productCategory;
    @ManyToOne
    @JoinColumn(name = "business_id")
    private Business business;
    private String name;
    private String description;
    private String imageUrl;
    private double price;
    private boolean active;
    
}
