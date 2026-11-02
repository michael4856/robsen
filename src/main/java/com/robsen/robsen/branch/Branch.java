package com.robsen.robsen.branch;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity 
@Data 
public class Branch {
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private Long id;
    private String branchCode;
    private String branchName;
    private String location;
    private String description;
}
