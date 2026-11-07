package com.robsen.robsen.warehouse;

import com.robsen.robsen.business.Business;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String warehouseName;

    @ManyToOne
    @JoinColumn(name = "business_id")
    private Business business;

    private String location;
    private String contactNumber;

    @Column(length = 24)
    private String status;
}
