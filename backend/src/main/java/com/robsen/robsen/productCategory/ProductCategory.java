package com.robsen.robsen.productCategory;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import com.robsen.robsen.business.Business;
import lombok.Data;

@Entity 
@Data 
public class ProductCategory {
    @Id 
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private boolean active;
    @ManyToOne
    @JoinColumn(name = "business_id")
    private Business business;
    
}
