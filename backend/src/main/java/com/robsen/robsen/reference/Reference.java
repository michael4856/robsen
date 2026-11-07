package com.robsen.robsen.reference;

import jakarta.persistence.*;
import lombok.Data;

@Entity 
@Data 
public class Reference {
    @Id 
    private Long id;
    private String referenceNumber;
    private ReferenceType referenceType;
    
}
