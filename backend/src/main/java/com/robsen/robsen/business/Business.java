package com.robsen.robsen.business;

import com.robsen.robsen.common.Status;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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

    @jakarta.persistence.Transient
    @JsonProperty("businessTypes")
    public List<String> getBusinessTypes() {
        if (businessType == null || businessType.isBlank()) return List.of();
        return Arrays.stream(businessType.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    @JsonProperty("businessTypes")
    public void setBusinessTypes(List<String> businessTypes) {
        this.businessType = BusinessType.normalize(businessTypes).stream().collect(Collectors.joining(","));
    }
}
