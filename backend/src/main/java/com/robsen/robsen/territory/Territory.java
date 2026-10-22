package com.robsen.robsen.territory;

import jakarta.persistence.*;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data 
public class Territory {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String region;
    private String city;
    private String description;
}
