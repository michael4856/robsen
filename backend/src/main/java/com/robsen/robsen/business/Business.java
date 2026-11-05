package com.robsen.robsen.business;

import com.robsen.robsen.common.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import lombok.Data;

@Data 
@Entity
@Table(name = "businesses")
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String legalName;
    private String tradeName;
    private String description;
    private String tinNumber;
    private String address;
    private String email;
    private String businessType;
    private String phoneNumber;
    @Enumerated(EnumType.STRING)
    private Status verificationStatus;

    @Column(nullable = false)
    private boolean active = true;
}
