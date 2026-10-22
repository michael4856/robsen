package com.robsen.robsen.productCategory;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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
    
}
