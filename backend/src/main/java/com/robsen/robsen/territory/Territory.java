package com.robsen.robsen.territory;

import com.robsen.robsen.business.Business;
import jakarta.persistence.*;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data 
public class Territory {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "business_id")
    private Business business;
    private String name;
    private String region;
    private String city;
    private String description;
}
