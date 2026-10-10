package com.robsen.robsen.tenant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String tenantId;

    private String tenantName;
    private String description;
    private String location;
    private boolean active;
}